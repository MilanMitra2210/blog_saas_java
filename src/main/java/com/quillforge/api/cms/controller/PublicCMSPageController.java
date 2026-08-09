package com.quillforge.api.cms.controller;

import com.quillforge.api.cms.dto.CMSPageResponse;
import com.quillforge.api.cms.dto.CMSPageMetricDto;
import com.quillforge.api.cms.service.CMSPageService;
import com.quillforge.api.common.dto.ApiResponse;
import com.quillforge.api.common.dto.PaginatedResponse;
import com.quillforge.api.common.service.AnalyticsBufferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/pages")
@Tag(name = "Storefront CMS Pages", description = "Public endpoints for CMS pages")
@RequiredArgsConstructor
public class PublicCMSPageController {

    private final CMSPageService cmsPageService;
    private final AnalyticsBufferService analyticsBufferService;

    @GetMapping
    @Operation(summary = "List CMS Pages", description = "Returns a paginated list of CMS pages, optionally filtered by status and search terms")
    public ResponseEntity<ApiResponse<PaginatedResponse<CMSPageResponse>>> getPages(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int limit) {

        Pageable pageable = PageRequest.of(page > 0 ? page - 1 : 0, limit, Sort.by("createdAt").descending());
        Page<CMSPageResponse> result = cmsPageService.getCMSPages(search, status, pageable);

        return ResponseEntity.ok(
                ApiResponse.success("CMS Pages retrieved successfully", PaginatedResponse.from(result))
        );
    }

    @GetMapping("/{*slug}")
    @Operation(summary = "Get CMS Page by slug", description = "Retrieves a single CMS Page's details by its URL slug")
    public ResponseEntity<ApiResponse<CMSPageResponse>> getPageBySlug(@PathVariable String slug) {
        CMSPageResponse response = cmsPageService.getCMSPageBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success("CMS Page retrieved successfully", response));
    }

    @PostMapping("/{pageId}/views")
    @Operation(summary = "Track CMS Page view metric with referrer")
    public ResponseEntity<ApiResponse<Void>> trackView(
            @PathVariable UUID pageId,
            @RequestParam(value = "referrer", required = false) String referrer,
            @RequestParam(value = "search", required = false) String search,
            jakarta.servlet.http.HttpServletRequest request
    ) {
        String ipAddress = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        String country = request.getHeader("X-Client-Country");
        analyticsBufferService.bufferView("cms_page", pageId, referrer, ipAddress, userAgent, search, country);
        return ResponseEntity.ok(ApiResponse.success("View queued for tracking successfully", null));
    }

    @PostMapping("/slug/views/{*slug}")
    @Operation(summary = "Track CMS Page view by slug")
    public ResponseEntity<ApiResponse<Void>> trackViewBySlug(
            @PathVariable String slug,
            @RequestParam(value = "referrer", required = false) String referrer,
            @RequestParam(value = "search", required = false) String search,
            jakarta.servlet.http.HttpServletRequest request
    ) {
        CMSPageResponse page = cmsPageService.getCMSPageBySlug(slug);
        String ipAddress = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        String country = request.getHeader("X-Client-Country");
        analyticsBufferService.bufferView("cms_page", page.getId(), referrer, ipAddress, userAgent, search, country);
        return ResponseEntity.ok(ApiResponse.success("View queued for tracking successfully", null));
    }

    @PostMapping("/{pageId}/read-progress")
    @Operation(summary = "Track CMS page read progression with engagement milestone")
    public ResponseEntity<ApiResponse<CMSPageMetricDto>> trackReadProgress(
            @PathVariable UUID pageId,
            @RequestParam(value = "milestone", defaultValue = "100") int milestone
    ) {
        CMSPageMetricDto response = cmsPageService.incrementMetric(pageId, "read_progress", milestone);
        return ResponseEntity.ok(ApiResponse.success("Read progress tracked successfully", response));
    }

    @PostMapping("/slug/read-progress/{*slug}")
    @Operation(summary = "Track CMS page read progress by slug with engagement milestone")
    public ResponseEntity<ApiResponse<CMSPageMetricDto>> trackReadProgressBySlug(
            @PathVariable String slug,
            @RequestParam(value = "milestone", defaultValue = "100") int milestone
    ) {
        CMSPageResponse page = cmsPageService.getCMSPageBySlug(slug);
        CMSPageMetricDto response = cmsPageService.incrementMetric(page.getId(), "read_progress", milestone);
        return ResponseEntity.ok(ApiResponse.success("Read progress tracked successfully", response));
    }

    @PostMapping("/slug/heartbeat/{*slug}")
    @Operation(summary = "Register visitor heartbeat for CMS page by slug")
    public ResponseEntity<ApiResponse<Void>> registerHeartbeatBySlug(
            @PathVariable String slug,
            @RequestParam String visitorId,
            jakarta.servlet.http.HttpServletRequest request
    ) {
        CMSPageResponse page = cmsPageService.getCMSPageBySlug(slug);
        String ipAddress = getClientIp(request);
        String country = request.getHeader("X-Client-Country");
        analyticsBufferService.registerHeartbeat("cms_page", page.getId(), visitorId, ipAddress, country);
        return ResponseEntity.ok(ApiResponse.success("Heartbeat registered successfully", null));
    }

    private String getClientIp(jakarta.servlet.http.HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
