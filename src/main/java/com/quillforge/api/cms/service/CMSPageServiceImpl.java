package com.quillforge.api.cms.service;

import com.quillforge.api.cms.dto.*;
import com.quillforge.api.cms.entity.*;
import com.quillforge.api.cms.repository.*;
import com.quillforge.api.cms.mapper.CMSPageMapper;
import com.quillforge.api.common.dto.ContentBlockDto;
import com.quillforge.api.common.entity.ContentBlock;
import com.quillforge.api.common.entity.SeoMetadata;
import com.quillforge.api.common.exception.BadRequestException;
import com.quillforge.api.common.exception.ResourceNotFoundException;
import com.quillforge.api.common.mapper.SeoMapper;
import com.quillforge.api.media.entity.Media;
import com.quillforge.api.media.mapper.MediaMapper;
import com.quillforge.api.media.repository.MediaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.cache.annotation.Cacheable;
import com.quillforge.api.common.service.RevalidationService;
import java.time.Instant;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CMSPageServiceImpl implements CMSPageService {

    private final RevalidationService revalidationService;

    private final CMSPageRepository cmsPageRepository;
    private final MediaRepository mediaRepository;
    private final CMSPageMapper cmsPageMapper;
    private final MediaMapper mediaMapper;
    private final SeoMapper seoMapper;
    private final CMSPageMetricRepository cmsPageMetricRepository;
    private final CMSPageViewLogRepository cmsPageViewLogRepository;

    @Override
    @Cacheable(value = "cms_pages_list", key = "'list:' + (#search ?: '') + '-' + (#status ?: '') + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<CMSPageResponse> getCMSPages(String search, String status, Pageable pageable) {
        Boolean isActive = null;
        if ("active".equalsIgnoreCase(status)) {
            isActive = true;
        } else if ("inactive".equalsIgnoreCase(status)) {
            isActive = false;
        }

        Page<CMSPage> cmsPage = cmsPageRepository.findAllFiltered(search, isActive, pageable);

        return cmsPage.map(p -> {
            CMSPageResponse dto = cmsPageMapper.toResponse(p);
            CMSPageMetric m = getOrCreateCMSPageMetric(p.getId());
            dto.setMetrics(cmsPageMapper.toDto(m));
            // Enrich with metaImage details if imageId exists
            if (p.getSeo() != null && p.getSeo().getMetaImageId() != null) {
                Media media = mediaRepository.findById(p.getSeo().getMetaImageId()).orElse(null);
                if (media != null && dto.getSeo() != null) {
                    dto.getSeo().setMetaImage(mediaMapper.toDto(media));
                }
            }
            return dto;
        });
    }

    @Override
    @Transactional
    public CMSPageResponse createCMSPage(CMSPageRequest request) {
        if (cmsPageRepository.existsBySlug(request.getSlug())) {
            throw new BadRequestException("CMS Page with slug " + request.getSlug() + " already exists");
        }

        CMSPage page = cmsPageMapper.toEntity(request);
        
        // Associate nested ContentBlocks with page ID
        if (page.getContentBlocks() != null) {
            for (ContentBlock block : page.getContentBlocks()) {
                block.setPageId(page.getId());
            }
        }

        CMSPage saved = cmsPageRepository.save(page);
        revalidationService.revalidate("cms_page", saved.getSlug(), "create");
        return cmsPageMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public CMSPageResponse updateCMSPage(UUID id, CMSPageRequest request) {
        CMSPage page = cmsPageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CMSPage", id));

        if (!page.getSlug().equals(request.getSlug()) && cmsPageRepository.existsBySlug(request.getSlug())) {
            throw new BadRequestException("CMS Page with slug " + request.getSlug() + " already exists");
        }

        page.setName(request.getName());
        page.setSlug(request.getSlug());
        page.setActive(request.isActive());

        // Update SEO Metadata
        if (request.getSeo() != null) {
            if (page.getSeo() == null) {
                page.setSeo(seoMapper.toEntity(request.getSeo()));
            } else {
                SeoMetadata seo = page.getSeo();
                seo.setMetaTitle(request.getSeo().getMetaTitle());
                seo.setMetaDescription(request.getSeo().getMetaDescription());
                seo.setMetaKeywords(request.getSeo().getMetaKeywords());
                seo.setMetaRobots(request.getSeo().getMetaRobots());
                seo.setMetaImageId(request.getSeo().getMetaImageId());
                seo.setJsonLd(request.getSeo().getJsonLd());
            }
        } else {
            page.setSeo(null);
        }

        // Update Content Blocks
        page.getContentBlocks().clear();
        if (request.getContentBlocks() != null) {
            for (ContentBlockDto dto : request.getContentBlocks()) {
                ContentBlock block = new ContentBlock();
                block.setBlockKey(dto.getBlockKey());
                block.setContent(dto.getContent());
                block.setPageId(page.getId());
                page.getContentBlocks().add(block);
            }
        }

        CMSPage updated = cmsPageRepository.save(page);
        revalidationService.revalidate("cms_page", updated.getSlug(), "update");
        return cmsPageMapper.toResponse(updated);
    }

    @Override
    public CMSPageResponse getCMSPageById(UUID id) {
        CMSPage page = cmsPageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CMSPage", id));
        
        CMSPageResponse dto = cmsPageMapper.toResponse(page);
        CMSPageMetric m = getOrCreateCMSPageMetric(page.getId());
        dto.setMetrics(cmsPageMapper.toDto(m));
        if (page.getSeo() != null && page.getSeo().getMetaImageId() != null) {
            Media media = mediaRepository.findById(page.getSeo().getMetaImageId()).orElse(null);
            if (media != null && dto.getSeo() != null) {
                dto.getSeo().setMetaImage(mediaMapper.toDto(media));
            }
        }
        return dto;
    }

    @Override
    @Cacheable(value = "cms_pages", key = "#slug")
    public CMSPageResponse getCMSPageBySlug(String slug) {
        CMSPage page = cmsPageRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("CMSPage with slug: " + slug));
        
        CMSPageResponse dto = cmsPageMapper.toResponse(page);
        CMSPageMetric m = getOrCreateCMSPageMetric(page.getId());
        dto.setMetrics(cmsPageMapper.toDto(m));
        if (page.getSeo() != null && page.getSeo().getMetaImageId() != null) {
            Media media = mediaRepository.findById(page.getSeo().getMetaImageId()).orElse(null);
            if (media != null && dto.getSeo() != null) {
                dto.getSeo().setMetaImage(mediaMapper.toDto(media));
            }
        }
        return dto;
    }

    @Override
    @Transactional
    public void deleteCMSPage(UUID id) {
        CMSPage page = cmsPageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CMSPage", id));

        page.setDeleted(true);
        page.setDeletedAt(Instant.now());
        CMSPage saved = cmsPageRepository.save(page);
        revalidationService.revalidate("cms_page", saved.getSlug(), "delete");
    }

    @Override
    @Transactional
    public CMSPageMetricDto incrementMetricWithAnalytics(UUID pageId, String metricType, String referrer, String ipAddress, String userAgent) {
        CMSPageMetric metric = getOrCreateCMSPageMetric(pageId);

        if ("view".equalsIgnoreCase(metricType)) {
            metric.setViews(metric.getViews() + 1);
            if (referrer != null && !referrer.isBlank()) {
                String refLower = referrer.toLowerCase();
                if (refLower.contains("google")) {
                    metric.setGoogleViews(metric.getGoogleViews() + 1);
                } else if (refLower.contains("twitter") || refLower.contains("t.co") || refLower.contains("x.com")) {
                    metric.setTwitterViews(metric.getTwitterViews() + 1);
                } else if (refLower.contains("linkedin")) {
                    metric.setLinkedinViews(metric.getLinkedinViews() + 1);
                } else {
                    metric.setOtherViews(metric.getOtherViews() + 1);
                }
            } else {
                metric.setDirectViews(metric.getDirectViews() + 1);
            }

            String ipHash = hashIpAddress(ipAddress);
            boolean isUnique = !cmsPageViewLogRepository.existsByPageIdAndIpHash(pageId, ipHash);
            if (isUnique && !"unknown".equals(ipHash)) {
                CMSPageViewLog viewLog = new CMSPageViewLog();
                viewLog.setPageId(pageId);
                viewLog.setIpHash(ipHash);
                cmsPageViewLogRepository.save(viewLog);

                metric.setUniqueViews(metric.getUniqueViews() + 1);

                String device = detectDevice(userAgent);
                if ("mobile".equals(device)) {
                    metric.setMobileViews(metric.getMobileViews() + 1);
                } else if ("tablet".equals(device)) {
                    metric.setTabletViews(metric.getTabletViews() + 1);
                } else {
                    metric.setDesktopViews(metric.getDesktopViews() + 1);
                }
            }
        } else {
            throw new BadRequestException("Invalid metric type");
        }

        CMSPageMetric saved = cmsPageMetricRepository.save(metric);
        String slug = cmsPageRepository.findById(pageId).map(CMSPage::getSlug).orElse(null);
        if (slug != null) {
            revalidationService.revalidate("cms_page", slug, "update");
        }
        return cmsPageMapper.toDto(saved);
    }

    @Override
    @Transactional
    public CMSPageMetricDto incrementMetric(UUID pageId, String metricType) {
        return incrementMetricWithAnalytics(pageId, metricType, null, null, null);
    }

    private CMSPageMetric getOrCreateCMSPageMetric(UUID pageId) {
        return cmsPageMetricRepository.findByPageId(pageId).orElseGet(() -> {
            CMSPageMetric m = new CMSPageMetric();
            m.setPageId(pageId);
            m.setViews(0);
            m.setGoogleViews(0);
            m.setTwitterViews(0);
            m.setLinkedinViews(0);
            m.setDirectViews(0);
            m.setOtherViews(0);
            m.setUniqueViews(0);
            m.setMobileViews(0);
            m.setTabletViews(0);
            m.setDesktopViews(0);
            return cmsPageMetricRepository.save(m);
        });
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

    private String detectDevice(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "desktop";
        }
        String ua = userAgent.toLowerCase();
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")) {
            return "mobile";
        }
        if (ua.contains("tablet") || ua.contains("ipad")) {
            return "tablet";
        }
        return "desktop";
    }
}
