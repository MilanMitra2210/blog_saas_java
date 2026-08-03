package com.quillforge.api.blog.service;

import com.quillforge.api.blog.dto.*;
import com.quillforge.api.common.dto.PaginatedResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface BlogService {

    // Categories
    PaginatedResponse<BlogCategoryDto> getCategories(int page, int limit, String search);
    List<BlogCategoryDto> getAllCategories();
    BlogCategoryDto createCategory(BlogCategoryDto dto);
    BlogCategoryDto updateCategory(UUID id, BlogCategoryDto dto);
    void deleteCategory(UUID id);

    // Authors
    PaginatedResponse<BlogAuthorDto> getAuthors(int page, int limit, String search);
    List<BlogAuthorDto> getAllAuthors();
    BlogAuthorDto createAuthor(BlogAuthorDto dto);
    BlogAuthorDto updateAuthor(UUID id, BlogAuthorDto dto);
    void deleteAuthor(UUID id);

    // Tags
    PaginatedResponse<TagDto> getTags(int page, int limit, String search);
    List<TagDto> getAllTags();
    TagDto createTag(TagDto dto);
    TagDto updateTag(UUID id, TagDto dto);
    void deleteTag(UUID id);

    // Blogs
    PaginatedResponse<BlogResponse> getBlogs(int page, int limit, String search, String status, UUID categoryId);
    BlogResponse getBlogById(UUID id);
    BlogResponse getBlogBySlug(String slug);
    BlogResponse createBlog(BlogRequest request);
    BlogResponse updateBlog(UUID id, BlogRequest request);
    void deleteBlog(UUID id);

    // Revisions
    List<BlogRevisionDto> getRevisions(UUID blogId);
    BlogResponse restoreRevision(UUID blogId, UUID revisionId);

    // Redirects
    Map<String, String> getRedirectMap();
    String getRedirectBySlug(String slug);
    void createManualRedirect(String oldSlug, UUID blogId);
    void deleteManualRedirect(String oldSlug);

    // Ratings & Metrics
    BlogResponse rateBlog(UUID blogId, double rating);
    BlogMetricDto incrementMetric(UUID blogId, String metricType);
    BlogMetricDto incrementMetricWithReferrer(UUID blogId, String metricType, String referrer);

    // Sitemap Data
    SitemapDataDto getSitemapData();
}
