package com.quillforge.api.blog.controller;

import com.quillforge.api.blog.dto.*;
import com.quillforge.api.blog.service.BlogService;
import com.quillforge.api.common.dto.ApiResponse;
import com.quillforge.api.common.dto.PaginatedResponse;
import com.quillforge.api.common.service.AnalyticsBufferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/blogs")
@Tag(name = "Public Blogs", description = "Public storefront endpoints for fetching blogs, categories, authors, and redirects")
@RequiredArgsConstructor
public class PublicBlogController {

    private final BlogService blogService;
    private final AnalyticsBufferService analyticsBufferService;

    @GetMapping
    @Operation(summary = "List blogs")
    public ResponseEntity<ApiResponse<PaginatedResponse<BlogResponse>>> getBlogs(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "category_id", required = false) String categoryParam,
            @RequestParam(value = "category_slug", required = false) String categorySlug,
            @RequestParam(value = "tag", required = false) String tag,
            @RequestParam(value = "tag_slug", required = false) String tagSlug,
            @RequestParam(value = "tag_id", required = false) String tagIdParam
    ) {
        String cat = categorySlug != null ? categorySlug : categoryParam;
        String tagParam = tag != null ? tag : (tagSlug != null ? tagSlug : tagIdParam);
        PaginatedResponse<BlogResponse> response = blogService.getBlogs(page, limit, search, status, cat, tagParam);
        return ResponseEntity.ok(ApiResponse.success("Blogs retrieved successfully", response));
    }

    @GetMapping("/categories")
    @Operation(summary = "List blog categories")
    public ResponseEntity<ApiResponse<List<BlogCategoryDto>>> getCategories() {
        List<BlogCategoryDto> categories = blogService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.success("Blog categories retrieved successfully", categories));
    }

    @GetMapping("/authors")
    @Operation(summary = "List blog authors")
    public ResponseEntity<ApiResponse<List<BlogAuthorDto>>> getAuthors() {
        List<BlogAuthorDto> authors = blogService.getAllAuthors();
        return ResponseEntity.ok(ApiResponse.success("Blog authors retrieved successfully", authors));
    }

    @GetMapping("/tags")
    @Operation(summary = "List blog tags")
    public ResponseEntity<ApiResponse<List<TagDto>>> getTags() {
        List<TagDto> tags = blogService.getAllTags();
        return ResponseEntity.ok(ApiResponse.success("Blog tags retrieved successfully", tags));
    }

    @GetMapping("/redirects/map")
    @Operation(summary = "Get blog redirect map")
    public ResponseEntity<ApiResponse<Map<String, String>>> getRedirectMap() {
        Map<String, String> map = blogService.getRedirectMap();
        return ResponseEntity.ok(ApiResponse.success("Blog redirects map retrieved successfully", map));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get blog post details by slug (handles 301 redirects)")
    public Object getBlogBySlug(@PathVariable String slug) {
        try {
            BlogResponse response = blogService.getBlogBySlug(slug);
            return ResponseEntity.ok(ApiResponse.success("Blog details retrieved successfully", response));
        } catch (Exception e) {
            String newSlug = blogService.getRedirectBySlug(slug);
            if (newSlug != null) {
                RedirectView redirect = new RedirectView("/blogs/" + newSlug);
                redirect.setStatusCode(org.springframework.http.HttpStatus.MOVED_PERMANENTLY);
                return redirect;
            }
            throw e;
        }
    }

    @PostMapping("/{blogId}/rate")
    @Operation(summary = "Rate a blog post")
    public ResponseEntity<ApiResponse<BlogResponse>> rateBlog(@PathVariable UUID blogId, @RequestBody Map<String, Double> payload) {
        Double rating = payload.get("rating");
        BlogResponse response = blogService.rateBlog(blogId, rating != null ? rating : 0.0);
        return ResponseEntity.ok(ApiResponse.success("Rating submitted successfully", response));
    }

    @PostMapping("/{blogId}/views")
    @Operation(summary = "Track post view metric with referrer")
    public ResponseEntity<ApiResponse<Void>> trackView(
            @PathVariable UUID blogId,
            @RequestParam(value = "referrer", required = false) String referrer,
            @RequestParam(value = "search", required = false) String search,
            jakarta.servlet.http.HttpServletRequest request
    ) {
        String ipAddress = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        String country = request.getHeader("X-Client-Country");
        analyticsBufferService.bufferView("blog", blogId, referrer, ipAddress, userAgent, search, country);
        return ResponseEntity.ok(ApiResponse.success("View queued for tracking successfully", null));
    }

    @PostMapping("/slug/views/{*slug}")
    @Operation(summary = "Track blog view by slug")
    public ResponseEntity<ApiResponse<Void>> trackViewBySlug(
            @PathVariable String slug,
            @RequestParam(value = "referrer", required = false) String referrer,
            @RequestParam(value = "search", required = false) String search,
            jakarta.servlet.http.HttpServletRequest request
    ) {
        BlogResponse blog = blogService.getBlogBySlug(slug);
        String ipAddress = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        String country = request.getHeader("X-Client-Country");
        analyticsBufferService.bufferView("blog", blog.getId(), referrer, ipAddress, userAgent, search, country);
        return ResponseEntity.ok(ApiResponse.success("View queued for tracking successfully", null));
    }

    @PostMapping("/slug/read-progress/{*slug}")
    @Operation(summary = "Track blog read progress by slug with engagement milestone")
    public ResponseEntity<ApiResponse<BlogMetricDto>> trackReadProgressBySlug(
            @PathVariable String slug,
            @RequestParam(value = "milestone", defaultValue = "100") int milestone
    ) {
        BlogResponse blog = blogService.getBlogBySlug(slug);
        BlogMetricDto response = blogService.incrementMetric(blog.getId(), "read_progress", milestone);
        return ResponseEntity.ok(ApiResponse.success("Read progress tracked successfully", response));
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

    @PostMapping("/{blogId}/likes")
    @Operation(summary = "Like a blog post metric")
    public ResponseEntity<ApiResponse<BlogMetricDto>> trackLike(@PathVariable UUID blogId) {
        BlogMetricDto response = blogService.incrementMetric(blogId, "like");
        return ResponseEntity.ok(ApiResponse.success("Like tracked successfully", response));
    }

    @PostMapping("/{blogId}/read-progress")
    @Operation(summary = "Track read progression metric with engagement milestone")
    public ResponseEntity<ApiResponse<BlogMetricDto>> trackReadProgress(
            @PathVariable UUID blogId,
            @RequestParam(value = "milestone", defaultValue = "100") int milestone
    ) {
        BlogMetricDto response = blogService.incrementMetric(blogId, "read_progress", milestone);
        return ResponseEntity.ok(ApiResponse.success("Read progress tracked successfully", response));
    }

    @GetMapping("/sitemap-data")
    @Operation(summary = "Get sitemap data (published blogs, categories, and CMS pages with last modified dates)")
    public ResponseEntity<ApiResponse<SitemapDataDto>> getSitemapData() {
        SitemapDataDto response = blogService.getSitemapData();
        return ResponseEntity.ok(ApiResponse.success("Sitemap data retrieved successfully", response));
    }
}
