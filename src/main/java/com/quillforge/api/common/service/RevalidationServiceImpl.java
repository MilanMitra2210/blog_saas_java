package com.quillforge.api.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class RevalidationServiceImpl implements RevalidationService {

    private final CacheManager cacheManager;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.revalidate.frontend-url}")
    private String frontendUrl;

    @Value("${app.revalidate.secret}")
    private String secret;

    private final HttpClient httpClient = HttpClient.newBuilder().build();

    @Override
    public void revalidate(String model, String slug, String action) {
        CompletableFuture.runAsync(() -> {
            try {
                // 1. Evict Spring Cache
                evictSpringCache(model, slug);

                // 2. Invalidate raw Redis keys
                invalidateRedisKeys(model, slug);

                // 3. Post to Next.js webhook
                triggerNextJsWebhook(model, slug, action);

            } catch (Exception e) {
                log.error("Revalidation failed for model: {}, slug: {}", model, slug, e);
            }
        });
    }

    private void evictSpringCache(String model, String slug) {
        try {
            if ("blog".equals(model) || "blog_category".equals(model) || "blog_tag".equals(model) || "blog_author".equals(model)) {
                clearCache("blogs_list");
                if (slug != null) {
                    evictKey("blogs", slug);
                }
            } else if ("cms_page".equals(model)) {
                clearCache("cms_pages_list");
                if (slug != null) {
                    evictKey("cms_pages", slug);
                }
            } else if ("company_settings".equals(model)) {
                clearCache("company_settings");
            }
        } catch (Exception e) {
            log.warn("Spring cache eviction failed", e);
        }
    }

    private void clearCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            log.info("Cleared cache: {}", cacheName);
        }
    }

    private void evictKey(String cacheName, String key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
            log.info("Evicted key: {} from cache: {}", key, cacheName);
        }
    }

    private void invalidateRedisKeys(String model, String slug) {
        try {
            if ("blog".equals(model) || "blog_category".equals(model) || "blog_tag".equals(model) || "blog_author".equals(model)) {
                deleteKeysByPattern("cache:get_blogs:*");
                deleteKeysByPattern("cache:get_blog_categories:*");
                deleteKeysByPattern("cache:get_tags:*");
                deleteKeysByPattern("cache:get_authors:*");
                if (slug != null) {
                    deleteKey("cache:get_blog_by_slug:slug=" + slug);
                }
            } else if ("cms_page".equals(model)) {
                deleteKeysByPattern("cache:get_all_pages:*");
                if (slug != null) {
                    deleteKey("cache:get_page_by_slug:slug=" + slug);
                }
            } else if ("company_settings".equals(model)) {
                deleteKeysByPattern("cache:get_company_settings*");
            }
        } catch (Exception e) {
            log.warn("Redis key invalidation failed", e);
        }
    }

    private void deleteKey(String key) {
        Boolean deleted = stringRedisTemplate.delete(key);
        if (Boolean.TRUE.equals(deleted)) {
            log.info("Deleted Redis key: {}", key);
        }
    }

    private void deleteKeysByPattern(String pattern) {
        Set<String> keys = stringRedisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
            log.info("Cleared {} keys matching pattern: {}", keys.size(), pattern);
        }
    }

    private void triggerNextJsWebhook(String model, String slug, String action) {
        try {
            String webhookUrl = frontendUrl + "/api/revalidate";
            Map<String, String> payload = Map.of(
                    "secret", secret,
                    "model", model,
                    "slug", slug != null ? slug : "",
                    "action", action
            );
            String jsonPayload = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() == 200) {
                            log.info("Next.js Cache revalidated successfully for model: {}, slug: {}", model, slug);
                        } else {
                            log.warn("Next.js Cache revalidation returned {}: {}", response.statusCode(), response.body());
                        }
                    })
                    .exceptionally(ex -> {
                        Throwable cause = ex.getCause();
                        if (cause instanceof java.net.ConnectException) {
                            log.error("Next.js storefront is offline or unreachable at {}. Webhook skipped: {}", webhookUrl, cause.getMessage());
                        } else {
                            log.error("Next.js Cache revalidation webhook failed for model: {}, slug: {}", model, slug, ex);
                        }
                        return null;
                    });
        } catch (Exception e) {
            log.error("Failed to build Next.js revalidation request", e);
        }
    }
}
