package com.quillforge.api.common.service;

import com.quillforge.api.blog.entity.Blog;
import com.quillforge.api.blog.repository.BlogRepository;
import com.quillforge.api.cms.entity.CMSPage;
import com.quillforge.api.cms.repository.CMSPageRepository;
import com.quillforge.api.common.entity.AnalyticsMetric;
import com.quillforge.api.common.entity.AnalyticsViewLog;
import com.quillforge.api.common.repository.AnalyticsMetricRepository;
import com.quillforge.api.common.repository.AnalyticsViewLogRepository;
import com.quillforge.api.common.repository.GeoMetricRepository;
import com.quillforge.api.common.repository.UtmCampaignMetricRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.quillforge.api.common.entity.EngagementMilestone;
import com.quillforge.api.common.repository.EngagementMilestoneRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsBufferServiceImpl implements AnalyticsBufferService {

    private final StringRedisTemplate redisTemplate;
    private final AnalyticsMetricRepository analyticsMetricRepository;
    private final AnalyticsViewLogRepository analyticsViewLogRepository;
    private final CMSPageRepository cmsPageRepository;
    private final BlogRepository blogRepository;
    private final RevalidationService revalidationService;
    private final UtmCampaignMetricRepository utmCampaignMetricRepository;
    private final EngagementMilestoneRepository engagementMilestoneRepository;
    private final GeoMetricRepository geoMetricRepository;

    @Value("${app.revalidate.frontend-url}")
    private String frontendUrl;

    @Override
    public void bufferView(String type, UUID entityId, String referrer, String ipAddress, String userAgent, String search, String country) {
        try {
            String pendingSetKey = "analytics:pending:" + type;
            String bufferKey = "analytics:buffer:" + type + ":" + entityId;

            // 1. Mark entity as pending sync
            redisTemplate.opsForSet().add(pendingSetKey, entityId.toString());

            // 2. Increment total views
            redisTemplate.opsForHash().increment(bufferKey, "views", 1);

            // 3. Determine and increment referrer metric
            String referrerSource = determineReferrerSource(referrer, search);
            redisTemplate.opsForHash().increment(bufferKey, referrerSource, 1);

            // 4. Determine and increment device metric
            String device = detectDevice(userAgent);
            redisTemplate.opsForHash().increment(bufferKey, device, 1);

            // 5. Handle unique view tracking
            String ipHash = hashIpAddress(ipAddress);
            boolean isUniqueView = false;
            if (!"unknown".equals(ipHash)) {
                String uniquesKey = "analytics:uniques:" + type + ":" + entityId;
                Long addedCount = redisTemplate.opsForSet().add(uniquesKey, ipHash);
                
                if (addedCount != null && addedCount > 0) {
                    isUniqueView = true;
                    redisTemplate.opsForHash().increment(bufferKey, "unique_views", 1);
                    
                    // Buffer IP logs to save to PG on flush
                    String logsKey = "analytics:logs:" + type + ":" + entityId;
                    redisTemplate.opsForSet().add(logsKey, ipHash);
                }
            }

            // 6. Handle UTM campaign view tracking
            String utmSource = getQueryParam(search, "utm_source");
            String utmMedium = getQueryParam(search, "utm_medium");
            String utmCampaign = getQueryParam(search, "utm_campaign");

            if (utmSource != null || utmMedium != null || utmCampaign != null) {
                String sourceVal = utmSource != null ? utmSource : "unknown";
                String mediumVal = utmMedium != null ? utmMedium : "unknown";
                String campaignVal = utmCampaign != null ? utmCampaign : "unknown";

                String utmKey = String.format("%s:%s:%s:%s:%s", type, entityId, sourceVal, mediumVal, campaignVal);
                redisTemplate.opsForSet().add("analytics:pending:utm", utmKey);

                String utmBufferKey = "analytics:buffer:utm:" + utmKey;
                redisTemplate.opsForHash().increment(utmBufferKey, "views", 1);
                if (isUniqueView) {
                    redisTemplate.opsForHash().increment(utmBufferKey, "unique_views", 1);
                }
            }

            // 7. Handle Geo view tracking
            String resolvedCountry = (country != null && !country.isEmpty()) ? country : resolveCountryFromIp(ipAddress);
            String geoKey = String.format("%s:%s:%s", type, entityId, resolvedCountry);
            redisTemplate.opsForSet().add("analytics:pending:geo", geoKey);

            String geoBufferKey = "analytics:buffer:geo:" + geoKey;
            redisTemplate.opsForHash().increment(geoBufferKey, "views", 1);
            if (isUniqueView) {
                redisTemplate.opsForHash().increment(geoBufferKey, "unique_views", 1);
            }
        } catch (Exception e) {
            log.error("Failed to buffer analytics view for {} ID: {}", type, entityId, e);
        }
    }

    @Override
    public void bufferReadProgress(String type, UUID entityId, int milestone) {
        try {
            String pendingSetKey = "analytics:pending:" + type;
            String bufferKey = "analytics:buffer:" + type + ":" + entityId;

            redisTemplate.opsForSet().add(pendingSetKey, entityId.toString());

            String field = switch (milestone) {
                case 25 -> "milestone_25";
                case 50 -> "milestone_50";
                case 75 -> "milestone_75";
                case 100 -> {
                    redisTemplate.opsForHash().increment(bufferKey, "read_progress", 1);
                    yield "milestone_100";
                }
                default -> null;
            };

            if (field != null) {
                redisTemplate.opsForHash().increment(bufferKey, field, 1);
            }
        } catch (Exception e) {
            log.error("Failed to buffer read progress for {} ID: {}, milestone: {}", type, entityId, milestone, e);
        }
    }

    @Override
    @Scheduled(fixedRate = 30000) // Flush every 30 seconds
    @Transactional
    public void flushMetrics() {
        flushEntityMetrics("cms_page");
        flushEntityMetrics("blog");
        flushUtmCampaignMetrics();
        flushGeoMetrics();
    }

    private void flushEntityMetrics(String type) {
        String pendingKey = "analytics:pending:" + type;
        Set<String> pendingIds = redisTemplate.opsForSet().members(pendingKey);
        if (pendingIds == null || pendingIds.isEmpty()) {
            return;
        }

        log.info("🚀 Flushing {} metrics buffer for {} pending items...", type, pendingIds.size());

        for (String idStr : pendingIds) {
            try {
                UUID entityId = UUID.fromString(idStr);
                String bufferKey = "analytics:buffer:" + type + ":" + entityId;
                Map<Object, Object> fields = redisTemplate.opsForHash().entries(bufferKey);
                if (fields.isEmpty()) {
                    continue;
                }

                // Verify entity exists and fetch slug
                String slug = null;
                if ("cms_page".equals(type)) {
                    CMSPage page = cmsPageRepository.findById(entityId).orElse(null);
                    if (page == null) {
                        cleanupKeys(type, entityId);
                        continue;
                    }
                    slug = page.getSlug();
                } else if ("blog".equals(type)) {
                    Blog blog = blogRepository.findById(entityId).orElse(null);
                    if (blog == null) {
                        cleanupKeys(type, entityId);
                        continue;
                    }
                    slug = blog.getSlug();
                }

                AnalyticsMetric metric = analyticsMetricRepository.findByEntityIdAndEntityType(entityId, type).orElseGet(() -> {
                    AnalyticsMetric m = new AnalyticsMetric();
                    m.setEntityId(entityId);
                    m.setEntityType(type);
                    m.setViews(0);
                    m.setLikes(0);
                    m.setReadProgressCount(0);
                    m.setGoogleViews(0);
                    m.setTwitterViews(0);
                    m.setLinkedinViews(0);
                    m.setDirectViews(0);
                    m.setOtherViews(0);
                    m.setUniqueViews(0);
                    m.setMobileViews(0);
                    m.setTabletViews(0);
                    m.setDesktopViews(0);
                    return analyticsMetricRepository.save(m);
                });

                // Apply updates
                metric.setViews(metric.getViews() + getIntValue(fields, "views"));
                metric.setLikes(metric.getLikes() + getIntValue(fields, "likes"));
                metric.setReadProgressCount(metric.getReadProgressCount() + getIntValue(fields, "read_progress"));
                metric.setGoogleViews(metric.getGoogleViews() + getIntValue(fields, "google_views"));
                metric.setTwitterViews(metric.getTwitterViews() + getIntValue(fields, "twitter_views"));
                metric.setLinkedinViews(metric.getLinkedinViews() + getIntValue(fields, "linkedin_views"));
                metric.setDirectViews(metric.getDirectViews() + getIntValue(fields, "direct_views"));
                metric.setOtherViews(metric.getOtherViews() + getIntValue(fields, "other_views"));
                metric.setUniqueViews(metric.getUniqueViews() + getIntValue(fields, "unique_views"));
                metric.setMobileViews(metric.getMobileViews() + getIntValue(fields, "mobile"));
                metric.setTabletViews(metric.getTabletViews() + getIntValue(fields, "tablet"));
                metric.setDesktopViews(metric.getDesktopViews() + getIntValue(fields, "desktop"));
                analyticsMetricRepository.save(metric);

                // Flush engagement milestones
                int m25 = getIntValue(fields, "milestone_25");
                int m50 = getIntValue(fields, "milestone_50");
                int m75 = getIntValue(fields, "milestone_75");
                int m100 = getIntValue(fields, "milestone_100");
                if (m25 > 0 || m50 > 0 || m75 > 0 || m100 > 0) {
                    String normalizedType = "cms_page".equals(type) ? "CMS" : "BLOG";
                    EngagementMilestone em = engagementMilestoneRepository.findByEntityIdAndEntityType(entityId, normalizedType)
                            .orElseGet(() -> {
                                EngagementMilestone newEm = new EngagementMilestone();
                                newEm.setEntityId(entityId);
                                newEm.setEntityType(normalizedType);
                                return newEm;
                            });
                    em.setMilestone25(em.getMilestone25() + m25);
                    em.setMilestone50(em.getMilestone50() + m50);
                    em.setMilestone75(em.getMilestone75() + m75);
                    em.setMilestone100(em.getMilestone100() + m100);
                    engagementMilestoneRepository.save(em);
                }

                // Write IP view logs
                String logsKey = "analytics:logs:" + type + ":" + entityId;
                Set<String> ipLogs = redisTemplate.opsForSet().members(logsKey);
                if (ipLogs != null) {
                    for (String ipHash : ipLogs) {
                        if (!analyticsViewLogRepository.existsByEntityIdAndEntityTypeAndIpHash(entityId, type, ipHash)) {
                            AnalyticsViewLog viewLog = new AnalyticsViewLog();
                            viewLog.setEntityId(entityId);
                            viewLog.setEntityType(type);
                            viewLog.setIpHash(ipHash);
                            analyticsViewLogRepository.save(viewLog);
                        }
                    }
                }

                // Evict local cache
                if (slug != null) {
                    revalidationService.evictSpringCache(type.equals("cms_page") ? "cms_page" : "blog", slug);
                }

                // Cleanup Redis
                cleanupKeys(type, entityId);

            } catch (Exception e) {
                log.error("Failed to flush {} metrics for ID: {}", type, idStr, e);
            }
        }
    }

    private int getIntValue(Map<Object, Object> fields, String key) {
        Object val = fields.get(key);
        if (val == null) return 0;
        try {
            return Integer.parseInt(val.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void cleanupKeys(String type, UUID entityId) {
        redisTemplate.delete("analytics:buffer:" + type + ":" + entityId);
        redisTemplate.delete("analytics:logs:" + type + ":" + entityId);
        redisTemplate.opsForSet().remove("analytics:pending:" + type, entityId.toString());
    }

    private String determineReferrerSource(String referrer, String search) {
        // 1. Check UTM parameters first (takes precedence for campaigns)
        if (search != null && !search.isBlank()) {
            String searchLower = search.toLowerCase();
            if (searchLower.contains("utm_source=google") || searchLower.contains("utm_medium=google") || searchLower.contains("utm_campaign=google")) {
                return "google_views";
            }
            if (searchLower.contains("utm_source=twitter") || searchLower.contains("utm_source=x.com") || searchLower.contains("utm_source=t.co") ||
                searchLower.contains("utm_medium=twitter") || searchLower.contains("utm_campaign=twitter")) {
                return "twitter_views";
            }
            if (searchLower.contains("utm_source=linkedin") || searchLower.contains("utm_medium=linkedin") || searchLower.contains("utm_campaign=linkedin")) {
                return "linkedin_views";
            }
        }

        // 2. Fallback to Referrer header check
        if (referrer == null || referrer.isBlank()) {
            return "direct_views";
        }

        String refLower = referrer.toLowerCase();

        // Dynamically detect internal navigation from our storefront site URL
        if (frontendUrl != null && !frontendUrl.isBlank()) {
            try {
                URI uri = new URI(frontendUrl);
                String host = uri.getHost();
                if (host != null && refLower.contains(host.toLowerCase())) {
                    return "direct_views";
                }
            } catch (Exception e) {
                // Fallback to local ports check
            }
        }

        if (refLower.contains("localhost:") || refLower.contains("127.0.0.1:")) {
            return "direct_views";
        }

        if (refLower.contains("google")) {
            return "google_views";
        } else if (refLower.contains("twitter") || refLower.contains("t.co") || refLower.contains("x.com")) {
            return "twitter_views";
        } else if (refLower.contains("linkedin")) {
            return "linkedin_views";
        } else {
            return "other_views";
        }
    }

    private String detectDevice(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "desktop";
        }
        String ua = userAgent.toLowerCase();
        
        // Tablet detection
        if (ua.contains("ipad") || ua.contains("playbook") || ua.contains("silk")) {
            return "tablet";
        }
        if (ua.contains("android") && !ua.contains("mobile")) {
            return "tablet";
        }
        
        // Mobile detection
        if (ua.contains("mobi") || ua.contains("iphone") || ua.contains("ipod") || 
            ua.contains("blackberry") || ua.contains("opera mini") || ua.contains("fennec") || 
            ua.contains("iemobile")) {
            return "mobile";
        }
        
        return "desktop";
    }

    private void flushUtmCampaignMetrics() {
        String pendingKey = "analytics:pending:utm";
        Set<String> pendingKeys = redisTemplate.opsForSet().members(pendingKey);
        if (pendingKeys == null || pendingKeys.isEmpty()) {
            return;
        }

        log.info("🚀 Flushing UTM Campaign metrics buffer for {} pending items...", pendingKeys.size());

        for (String utmKey : pendingKeys) {
            try {
                // Format: <entityType>:<entityId>:<source>:<medium>:<campaign>
                String[] parts = utmKey.split(":");
                if (parts.length < 5) {
                    redisTemplate.opsForSet().remove(pendingKey, utmKey);
                    continue;
                }

                String entityType = parts[0];
                UUID entityId = UUID.fromString(parts[1]);
                String utmSource = parts[2];
                String utmMedium = parts[3];
                String utmCampaign = parts[4];

                String utmBufferKey = "analytics:buffer:utm:" + utmKey;
                Map<Object, Object> fields = redisTemplate.opsForHash().entries(utmBufferKey);
                if (fields.isEmpty()) {
                    continue;
                }

                com.quillforge.api.common.entity.UtmCampaignMetric metric = utmCampaignMetricRepository
                        .findByEntityIdAndEntityTypeAndUtmSourceAndUtmMediumAndUtmCampaign(
                                entityId, entityType, utmSource, utmMedium, utmCampaign
                        ).orElseGet(() -> {
                            com.quillforge.api.common.entity.UtmCampaignMetric m = new com.quillforge.api.common.entity.UtmCampaignMetric();
                            m.setEntityId(entityId);
                            m.setEntityType(entityType);
                            m.setUtmSource("unknown".equals(utmSource) ? null : utmSource);
                            m.setUtmMedium("unknown".equals(utmMedium) ? null : utmMedium);
                            m.setUtmCampaign("unknown".equals(utmCampaign) ? null : utmCampaign);
                            m.setViews(0);
                            m.setUniqueViews(0);
                            return utmCampaignMetricRepository.save(m);
                        });

                metric.setViews(metric.getViews() + getIntValue(fields, "views"));
                metric.setUniqueViews(metric.getUniqueViews() + getIntValue(fields, "unique_views"));
                utmCampaignMetricRepository.save(metric);

                // Cleanup Redis
                redisTemplate.delete(utmBufferKey);
                redisTemplate.opsForSet().remove(pendingKey, utmKey);

            } catch (Exception e) {
                log.error("Failed to flush UTM metrics for key: {}", utmKey, e);
            }
        }
    }

    private String getQueryParam(String search, String paramName) {
        if (search == null || search.isBlank()) return null;
        String query = search.startsWith("?") ? search.substring(1) : search;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx > 0) {
                try {
                    String key = java.net.URLDecoder.decode(pair.substring(0, idx), java.nio.charset.StandardCharsets.UTF_8.name());
                    if (key.equalsIgnoreCase(paramName)) {
                        return java.net.URLDecoder.decode(pair.substring(idx + 1), java.nio.charset.StandardCharsets.UTF_8.name());
                    }
                } catch (Exception e) {
                    // ignore
                }
            }
        }
        return null;
    }

    private String hashIpAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return "unknown";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(ipAddress.trim().getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String resolveCountryFromIp(String ip) {
        if (ip == null || ip.isEmpty() || ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1") || ip.equalsIgnoreCase("localhost")) {
            String[] demoCountries = {"US", "IN", "DE", "FR", "GB", "CA", "AU"};
            int idx = Math.abs(ip == null ? 0 : ip.hashCode()) % demoCountries.length;
            return demoCountries[idx];
        }
        return "unknown";
    }

    private void flushGeoMetrics() {
        String pendingKey = "analytics:pending:geo";
        Set<String> pendingKeys = redisTemplate.opsForSet().members(pendingKey);
        if (pendingKeys == null || pendingKeys.isEmpty()) {
            return;
        }

        log.info("🚀 Flushing Geo metrics buffer for {} pending items...", pendingKeys.size());

        for (String geoKey : pendingKeys) {
            try {
                // Format: <entityType>:<entityId>:<countryCode>
                String[] parts = geoKey.split(":");
                if (parts.length < 3) {
                    redisTemplate.opsForSet().remove(pendingKey, geoKey);
                    continue;
                }

                String entityType = parts[0];
                UUID entityId = UUID.fromString(parts[1]);
                String countryCode = parts[2];

                String geoBufferKey = "analytics:buffer:geo:" + geoKey;
                Map<Object, Object> fields = redisTemplate.opsForHash().entries(geoBufferKey);
                if (fields.isEmpty()) {
                    continue;
                }

                com.quillforge.api.common.entity.GeoMetric metric = geoMetricRepository
                        .findByEntityIdAndEntityTypeAndCountryCode(entityId, entityType, countryCode)
                        .orElseGet(() -> {
                            com.quillforge.api.common.entity.GeoMetric m = new com.quillforge.api.common.entity.GeoMetric();
                            m.setEntityId(entityId);
                            m.setEntityType(entityType);
                            m.setCountryCode(countryCode);
                            m.setViews(0);
                            m.setUniqueViews(0);
                            return geoMetricRepository.save(m);
                        });

                metric.setViews(metric.getViews() + getIntValue(fields, "views"));
                metric.setUniqueViews(metric.getUniqueViews() + getIntValue(fields, "unique_views"));
                geoMetricRepository.save(metric);

                // Cleanup Redis
                redisTemplate.delete(geoBufferKey);
                redisTemplate.opsForSet().remove(pendingKey, geoKey);

            } catch (Exception e) {
                log.error("Failed to flush Geo metrics for key: {}", geoKey, e);
            }
        }
    }
}
