package com.quillforge.api.blog.controller;

import com.quillforge.api.blog.entity.Blog;
import com.quillforge.api.blog.entity.BlogComment;
import com.quillforge.api.blog.entity.BlogMetric;
import com.quillforge.api.blog.repository.BlogRepository;
import com.quillforge.api.blog.repository.BlogCommentRepository;
import com.quillforge.api.blog.repository.BlogMetricRepository;
import com.quillforge.api.common.dto.ApiResponse;
import com.quillforge.api.common.dto.PaginatedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.quillforge.api.enquiry.entity.Enquiry;
import com.quillforge.api.enquiry.repository.EnquiryRepository;
import com.quillforge.api.enquiry.mapper.EnquiryMapper;
import com.quillforge.api.enquiry.dto.EnquiryResponseDto;

import org.springframework.transaction.annotation.Transactional;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/dashboard")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Dashboard Analytics", description = "Endpoints for fetching platform stats and post analytics")
@RequiredArgsConstructor
public class DashboardController {

    private final BlogRepository blogRepository;
    private final BlogCommentRepository blogCommentRepository;
    private final BlogMetricRepository blogMetricRepository;
    private final EnquiryRepository enquiryRepository;
    private final EnquiryMapper enquiryMapper;

    @GetMapping("/stats")
    @Transactional(readOnly = true)
    @Operation(summary = "Fetch dashboard metrics, recent activity, and analytics summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats() {
        long totalBlogs = blogRepository.count();
        long totalComments = blogCommentRepository.count();

        long totalEnquiries = enquiryRepository.count();
        long pendingEnquiries = enquiryRepository.countByStatus("pending");

        List<Map<String, Object>> recentBlogs = new ArrayList<>();
        List<Blog> recentBlogsList = blogRepository.findAll(PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();
        for (Blog b : recentBlogsList) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", b.getId().toString());
            map.put("title", b.getTitle());
            map.put("categoryName", b.getCategory() != null ? b.getCategory().getName() : null);
            map.put("slug", b.getSlug());
            map.put("status", b.isPublished() ? "Published" : "Draft");
            map.put("createdAt", b.getCreatedAt() != null ? b.getCreatedAt().toString() : null);
            recentBlogs.add(map);
        }

        List<EnquiryResponseDto> recentEnquiries = enquiryRepository.findAll(PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent()
                .stream()
                .map(enquiryMapper::toDto)
                .toList();

        // Aggregate stats
        List<BlogMetric> metrics = new ArrayList<>();
        for (Blog b : blogRepository.findAll()) {
            metrics.add(getOrCreateBlogMetric(b.getId()));
        }
        long totalViews = 0;
        long totalLikes = 0;
        long totalCompletions = 0;
        long googleViews = 0;
        long twitterViews = 0;
        long linkedinViews = 0;
        long directViews = 0;
        long otherViews = 0;
        for (BlogMetric m : metrics) {
            totalViews += m.getViews();
            totalLikes += m.getLikes();
            totalCompletions += m.getReadProgressCount();
            googleViews += m.getGoogleViews();
            twitterViews += m.getTwitterViews();
            linkedinViews += m.getLinkedinViews();
            directViews += m.getDirectViews();
            otherViews += m.getOtherViews();
        }

        long overallReach = totalViews + (totalComments * 5) + (totalLikes * 2);

        // Fetch Top 5 performing posts
        List<BlogMetric> sortedMetrics = new ArrayList<>(metrics);
        sortedMetrics.sort((m1, m2) -> Integer.compare(m2.getViews(), m1.getViews()));
        List<BlogMetric> top5Metrics = sortedMetrics.subList(0, Math.min(5, sortedMetrics.size()));

        List<Map<String, Object>> topPosts = new ArrayList<>();
        for (BlogMetric m : top5Metrics) {
            Optional<Blog> optBlog = blogRepository.findById(m.getBlogId());
            if (optBlog.isPresent()) {
                Blog b = optBlog.get();
                if (b.isPublished()) {
                    // Count comments manually for this post
                    long commentsCount = blogCommentRepository.findAll().stream()
                            .filter(c -> b.getId().equals(c.getPostId()))
                            .count();

                    double completionRate = m.getViews() > 0 ? Math.round((double) m.getReadProgressCount() / m.getViews() * 1000.0) / 10.0 : 0.0;

                    Map<String, Object> map = new HashMap<>();
                    map.put("id", b.getId().toString());
                    map.put("title", b.getTitle());
                    map.put("slug", b.getSlug());
                    map.put("views", m.getViews());
                    map.put("likes", m.getLikes());
                    map.put("comments", commentsCount);
                    map.put("completions", m.getReadProgressCount());
                    map.put("completionRate", completionRate);
                    topPosts.add(map);
                }
            }
        }

        double avgCompletionRate = totalViews > 0 ? Math.round((double) totalCompletions / totalViews * 1000.0) / 10.0 : 0.0;

        // Generate historical 30-day timeline (deterministic random seed matches python-backend)
        List<Map<String, Object>> timeline = new ArrayList<>();
        Instant today = Instant.now();
        Random random = new Random(42);

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.US);

        for (int i = 30; i > 0; i--) {
            Instant targetInstant = today.minus(i, ChronoUnit.DAYS);
            Date targetDate = Date.from(targetInstant);

            double viewFactor = 0.6 + (1.4 - 0.6) * random.nextDouble();
            double likeFactor = 0.5 + (1.5 - 0.5) * random.nextDouble();
            double commentFactor = 0.4 + (1.6 - 0.4) * random.nextDouble();

            long dailyViews = Math.round((double) totalViews / 30 * viewFactor);
            long dailyLikes = Math.round((double) totalLikes / 30 * likeFactor);
            long dailyComments = Math.round((double) totalComments / 30 * commentFactor);
            long dailyReach = dailyViews + (dailyComments * 5) + (dailyLikes * 2);

            Map<String, Object> map = new HashMap<>();
            map.put("date", sdf.format(targetDate));
            map.put("views", dailyViews);
            map.put("likes", dailyLikes);
            map.put("comments", dailyComments);
            map.put("reach", dailyReach);
            timeline.add(map);
        }

        Map<String, Object> analytics = new HashMap<>();
        analytics.put("totalViews", totalViews);
        analytics.put("totalLikes", totalLikes);
        analytics.put("totalComments", totalComments);
        analytics.put("totalCompletions", totalCompletions);
        analytics.put("averageCompletionRate", avgCompletionRate);
        analytics.put("overallReach", overallReach);
        analytics.put("googleViews", googleViews);
        analytics.put("twitterViews", twitterViews);
        analytics.put("linkedinViews", linkedinViews);
        analytics.put("directViews", directViews + otherViews);
        analytics.put("topPosts", topPosts);
        analytics.put("timeline", timeline);

        Map<String, Object> data = new HashMap<>();
        data.put("totalBlogs", totalBlogs);
        data.put("totalEnquiries", totalEnquiries);
        data.put("pendingEnquiries", pendingEnquiries);
        data.put("recentBlogs", recentBlogs);
        data.put("recentEnquiries", recentEnquiries);
        data.put("analytics", analytics);

        return ResponseEntity.ok(ApiResponse.success("Dashboard stats and analytics retrieved successfully", data));
    }

    @GetMapping("/analytics/posts")
    @Transactional(readOnly = true)
    @Operation(summary = "Get paginated list of blog metrics breakdown")
    public ResponseEntity<ApiResponse<PaginatedResponse<Map<String, Object>>>> getPaginatedBlogAnalytics(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "sortBy", defaultValue = "views") String sortBy
    ) {
        // Fetch all published blogs
        List<Blog> blogs = blogRepository.findByIsPublishedTrue();

        // Filter by search query
        if (search != null && !search.trim().isEmpty()) {
            String lowerSearch = search.trim().toLowerCase();
            blogs = blogs.stream()
                    .filter(b -> b.getTitle().toLowerCase().contains(lowerSearch))
                    .toList();
        }

        // Map blogs to metrics list
        List<Map<String, Object>> items = new ArrayList<>();
        for (Blog b : blogs) {
            BlogMetric metric = getOrCreateBlogMetric(b.getId());
            int views = metric.getViews();
            int likes = metric.getLikes();
            int completions = metric.getReadProgressCount();

            long commentsCount = blogCommentRepository.findAll().stream()
                    .filter(c -> b.getId().equals(c.getPostId()))
                    .count();

            double rate = views > 0 ? Math.round((double) completions / views * 1000.0) / 10.0 : 0.0;

            Map<String, Object> item = new HashMap<>();
            item.put("id", b.getId().toString());
            item.put("title", b.getTitle());
            item.put("slug", b.getSlug());
            item.put("views", views);
            item.put("likes", likes);
            item.put("comments", commentsCount);
            item.put("completions", completions);
            item.put("completionRate", rate);
            item.put("googleViews", metric.getGoogleViews());
            item.put("twitterViews", metric.getTwitterViews());
            item.put("linkedinViews", metric.getLinkedinViews());
            item.put("directViews", metric.getDirectViews() + metric.getOtherViews());
            items.add(item);
        }

        // Sort items
        if ("likes".equalsIgnoreCase(sortBy)) {
            items.sort((i1, i2) -> Integer.compare((Integer) i2.get("likes"), (Integer) i1.get("likes")));
        } else if ("completions".equalsIgnoreCase(sortBy)) {
            items.sort((i1, i2) -> Integer.compare((Integer) i2.get("completions"), (Integer) i1.get("completions")));
        } else {
            items.sort((i1, i2) -> Integer.compare((Integer) i2.get("views"), (Integer) i1.get("views")));
        }

        // Paginate
        int total = items.size();
        int fromIndex = (page - 1) * limit;
        int toIndex = Math.min(fromIndex + limit, total);

        List<Map<String, Object>> paginatedItems = new ArrayList<>();
        if (fromIndex < total) {
            paginatedItems = items.subList(fromIndex, toIndex);
        }

        int totalPages = (int) Math.ceil((double) total / limit);

        PaginatedResponse<Map<String, Object>> response = PaginatedResponse.<Map<String, Object>>builder()
                .items(paginatedItems)
                .page(page)
                .limit(limit)
                .total(total)
                .totalPages(totalPages)
                .build();

        return ResponseEntity.ok(ApiResponse.success("Blog posts metrics retrieved successfully", response));
    }

    private BlogMetric getOrCreateBlogMetric(UUID blogId) {
        return blogMetricRepository.findByBlogId(blogId).orElseGet(() -> {
            BlogMetric m = new BlogMetric();
            m.setBlogId(blogId);
            m.setViews((int) (Math.random() * 500) + 120);
            m.setLikes((int) (m.getViews() * (Math.random() * 0.15 + 0.05)));
            m.setReadProgressCount((int) (m.getViews() * (Math.random() * 0.40 + 0.30)));
            m.setGoogleViews((int) (m.getViews() * 0.4));
            m.setTwitterViews((int) (m.getViews() * 0.2));
            m.setLinkedinViews((int) (m.getViews() * 0.2));
            m.setDirectViews((int) (m.getViews() * 0.1));
            m.setOtherViews((int) (m.getViews() * 0.1));
            return blogMetricRepository.save(m);
        });
    }
}
