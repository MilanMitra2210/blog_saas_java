package com.quillforge.api.blog.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quillforge.api.blog.dto.*;
import com.quillforge.api.blog.entity.*;
import com.quillforge.api.cms.entity.CMSPage;
import com.quillforge.api.blog.mapper.BlogMapper;
import com.quillforge.api.blog.repository.*;
import com.quillforge.api.common.dto.PaginatedResponse;
import com.quillforge.api.common.entity.SeoMetadata;
import com.quillforge.api.common.exception.BadRequestException;
import com.quillforge.api.common.exception.ResourceNotFoundException;
import com.quillforge.api.common.mapper.SeoMapper;
import com.quillforge.api.media.entity.Media;
import com.quillforge.api.media.repository.MediaRepository;
import com.quillforge.api.cms.repository.CMSPageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class BlogServiceImpl implements BlogService {

    private final BlogCategoryService blogCategoryService;
    private final BlogAuthorService blogAuthorService;
    private final TagService tagService;

    private final BlogRepository blogRepository;
    private final BlogCategoryRepository blogCategoryRepository;
    private final BlogAuthorRepository blogAuthorRepository;
    private final TagRepository tagRepository;
    private final BlogMetricRepository blogMetricRepository;
    private final BlogRevisionRepository blogRevisionRepository;
    private final BlogSlugRedirectRepository blogSlugRedirectRepository;
    private final MediaRepository mediaRepository;
    private final CMSPageRepository cmsPageRepository;

    private final BlogMapper blogMapper;
    private final SeoMapper seoMapper;
    private final ObjectMapper objectMapper;

    // ---- Categories (Delegated) ----

    @Override
    public PaginatedResponse<BlogCategoryDto> getCategories(int page, int limit, String search) {
        return blogCategoryService.getCategories(page, limit, search);
    }

    @Override
    public List<BlogCategoryDto> getAllCategories() {
        return blogCategoryService.getAllCategories();
    }

    @Override
    @Transactional
    public BlogCategoryDto createCategory(BlogCategoryDto dto) {
        return blogCategoryService.createCategory(dto);
    }

    @Override
    @Transactional
    public BlogCategoryDto updateCategory(UUID id, BlogCategoryDto dto) {
        return blogCategoryService.updateCategory(id, dto);
    }

    @Override
    @Transactional
    public void deleteCategory(UUID id) {
        blogCategoryService.deleteCategory(id);
    }

    // ---- Authors (Delegated) ----

    @Override
    public PaginatedResponse<BlogAuthorDto> getAuthors(int page, int limit, String search) {
        return blogAuthorService.getAuthors(page, limit, search);
    }

    @Override
    public List<BlogAuthorDto> getAllAuthors() {
        return blogAuthorService.getAllAuthors();
    }

    @Override
    @Transactional
    public BlogAuthorDto createAuthor(BlogAuthorDto dto) {
        return blogAuthorService.createAuthor(dto);
    }

    @Override
    @Transactional
    public BlogAuthorDto updateAuthor(UUID id, BlogAuthorDto dto) {
        return blogAuthorService.updateAuthor(id, dto);
    }

    @Override
    @Transactional
    public void deleteAuthor(UUID id) {
        blogAuthorService.deleteAuthor(id);
    }

    // ---- Tags (Delegated) ----

    @Override
    public PaginatedResponse<TagDto> getTags(int page, int limit, String search) {
        return tagService.getTags(page, limit, search);
    }

    @Override
    public List<TagDto> getAllTags() {
        return tagService.getAllTags();
    }

    @Override
    @Transactional
    public TagDto createTag(TagDto dto) {
        return tagService.createTag(dto);
    }

    @Override
    @Transactional
    public TagDto updateTag(UUID id, TagDto dto) {
        return tagService.updateTag(id, dto);
    }

    @Override
    @Transactional
    public void deleteTag(UUID id) {
        tagService.deleteTag(id);
    }

    // ---- Blogs ----

    @Override
    public PaginatedResponse<BlogResponse> getBlogs(int page, int limit, String search, String status, UUID categoryId) {
        Boolean isPublished = null;
        if ("published".equalsIgnoreCase(status)) {
            isPublished = true;
        } else if ("draft".equalsIgnoreCase(status)) {
            isPublished = false;
        }

        PageRequest pageRequest = PageRequest.of(page - 1, limit, Sort.by("displayOrder").ascending().and(Sort.by("createdAt").descending()));
        Page<Blog> blogPage = blogRepository.findAllFiltered(search, isPublished, categoryId, pageRequest);

        return PaginatedResponse.of(blogPage, b -> {
            BlogResponse res = blogMapper.toResponse(b);
            blogMetricRepository.findByBlogId(b.getId()).ifPresent(m -> res.setMetrics(blogMapper.toDto(m)));
            return res;
        });
    }

    @Override
    public BlogResponse getBlogById(UUID id) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blog", id));
        BlogResponse res = blogMapper.toResponse(blog);
        blogMetricRepository.findByBlogId(blog.getId()).ifPresent(m -> res.setMetrics(blogMapper.toDto(m)));
        return res;
    }

    @Override
    public BlogResponse getBlogBySlug(String slug) {
        Blog blog = blogRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Blog with slug: " + slug));
        BlogResponse res = blogMapper.toResponse(blog);
        blogMetricRepository.findByBlogId(blog.getId()).ifPresent(m -> res.setMetrics(blogMapper.toDto(m)));
        return res;
    }

    @Override
    @Transactional
    public BlogResponse createBlog(BlogRequest request) {
        if (blogRepository.existsBySlug(request.getSlug())) {
            throw new BadRequestException("Blog with slug '" + request.getSlug() + "' already exists");
        }

        Blog blog = new Blog();
        mapRequestToBlog(request, blog);

        Blog saved = blogRepository.save(blog);

        // Initialise metrics
        BlogMetric metric = new BlogMetric();
        metric.setBlogId(saved.getId());
        blogMetricRepository.save(metric);

        return getBlogById(saved.getId());
    }

    @Override
    @Transactional
    public BlogResponse updateBlog(UUID id, BlogRequest request) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blog", id));

        // Create revision backup (only if not completely blank draft)
        boolean isBlank = (blog.getTitle() == null || blog.getTitle().isBlank()) && (blog.getExcerpt() == null || blog.getExcerpt().isBlank()) && blog.getSections().isEmpty();
        if (!isBlank) {
            long revCount = blogRevisionRepository.countByBlogId(blog.getId());
            BlogRevision revision = new BlogRevision();
            revision.setBlogId(blog.getId());
            revision.setTitle(blog.getTitle() != null ? blog.getTitle() : "");
            revision.setExcerpt(blog.getExcerpt());
            revision.setRevisionNumber((int) (revCount + 1));
            revision.setSectionsData(serializeBlogRevisionData(blog));
            blogRevisionRepository.save(revision);
        }

        // Handle slug redirects
        String newSlug = request.getSlug();
        if (newSlug != null && !newSlug.equals(blog.getSlug())) {
            blogRepository.findBySlug(newSlug).ifPresent(collision -> {
                if (!collision.getId().equals(id)) {
                    throw new BadRequestException("Blog with slug '" + newSlug + "' already exists");
                }
            });
            String oldSlug = blog.getSlug();
            blogSlugRedirectRepository.findByOldSlug(newSlug).ifPresent(blogSlugRedirectRepository::delete);
            blogSlugRedirectRepository.findByOldSlug(oldSlug).ifPresent(blogSlugRedirectRepository::delete);

            BlogSlugRedirect redirect = new BlogSlugRedirect();
            redirect.setOldSlug(oldSlug);
            redirect.setBlogId(blog.getId());
            blogSlugRedirectRepository.save(redirect);
        }

        // Map updates
        mapRequestToBlog(request, blog);

        blogRepository.save(blog);
        return getBlogById(blog.getId());
    }

    @Override
    @Transactional
    public void deleteBlog(UUID id) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blog", id));
        blog.setDeleted(true);
        blog.setDeletedAt(Instant.now());
        blogRepository.save(blog);
    }

    // ---- Revisions ----

    @Override
    public List<BlogRevisionDto> getRevisions(UUID blogId) {
        return blogRevisionRepository.findByBlogIdOrderByRevisionNumberDesc(blogId)
                .stream()
                .map(blogMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public BlogResponse restoreRevision(UUID blogId, UUID revisionId) {
        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new ResourceNotFoundException("Blog", blogId));

        BlogRevision revision = blogRevisionRepository.findById(revisionId)
                .orElseThrow(() -> new ResourceNotFoundException("Revision", revisionId));

        // Backup current state
        long revCount = blogRevisionRepository.countByBlogId(blog.getId());
        BlogRevision backup = new BlogRevision();
        backup.setBlogId(blog.getId());
        backup.setTitle(blog.getTitle());
        backup.setExcerpt(blog.getExcerpt());
        backup.setRevisionNumber((int) (revCount + 1));
        backup.setSectionsData(serializeBlogRevisionData(blog));
        blogRevisionRepository.save(backup);

        // Restore fields
        blog.setTitle(revision.getTitle());
        blog.setExcerpt(revision.getExcerpt());

        try {
            Map<String, Object> data = objectMapper.convertValue(revision.getSectionsData(), Map.class);
            if (data != null) {
                Map<String, Object> settings = (Map<String, Object>) data.get("basic_settings");
                if (settings != null) {
                    blog.setSlug((String) settings.get("slug"));
                    blog.setFeatured(Boolean.TRUE.equals(settings.get("is_featured")));
                    blog.setPublished(Boolean.TRUE.equals(settings.get("is_published")));

                    String catId = (String) settings.get("category_id");
                    blog.setCategory(catId != null ? blogCategoryRepository.findById(UUID.fromString(catId)).orElse(null) : null);

                    String authId = (String) settings.get("author_id");
                    blog.setAuthor(authId != null ? blogAuthorRepository.findById(UUID.fromString(authId)).orElse(null) : null);

                    List<String> tagIds = (List<String>) settings.get("tag_ids");
                    if (tagIds != null) {
                        blog.setTags(tagRepository.findAllById(tagIds.stream().map(UUID::fromString).toList()));
                    }
                }

                Map<String, Object> seoData = (Map<String, Object>) data.get("seo_data");
                if (seoData != null) {
                    if (blog.getSeo() == null) {
                        blog.setSeo(new SeoMetadata());
                    }
                    blog.getSeo().setMetaTitle((String) seoData.get("meta_title"));
                    blog.getSeo().setMetaDescription((String) seoData.get("meta_description"));
                    blog.getSeo().setMetaKeywords((String) seoData.get("meta_keywords"));
                }

                List<Map<String, Object>> sectionsList = (List<Map<String, Object>>) data.get("sections");
                if (sectionsList != null) {
                    blog.getSections().clear();
                    for (Map<String, Object> secMap : sectionsList) {
                        BlogSection sec = new BlogSection();
                        sec.setTitle((String) secMap.get("title"));
                        sec.setContent(secMap.get("content"));
                        sec.setSectionType((String) secMap.get("section_type"));
                        sec.setDisplayOrder((int) secMap.get("display_order"));
                        String mId = (String) secMap.get("media_id");
                        sec.setMedia(mId != null ? mediaRepository.findById(UUID.fromString(mId)).orElse(null) : null);
                        sec.setCtaButtonText((String) secMap.get("cta_button_text"));
                        sec.setCtaButtonUrl((String) secMap.get("cta_button_url"));
                        sec.setEmbedUrl((String) secMap.get("embed_url"));
                        sec.setEmbedProvider((String) secMap.get("embed_provider"));
                        sec.setCodeBlock((String) secMap.get("code_block"));
                        sec.setCodeLanguage((String) secMap.get("code_language"));

                        List<Map<String, Object>> subList = (List<Map<String, Object>>) secMap.get("sub_sections");
                        if (subList != null) {
                            for (Map<String, Object> subMap : subList) {
                                BlogSubSection sub = new BlogSubSection();
                                sub.setTitle((String) subMap.get("title"));
                                sub.setContent(subMap.get("content"));
                                sub.setDisplayOrder((int) subMap.get("display_order"));
                                String smId = (String) subMap.get("media_id");
                                sub.setMedia(smId != null ? mediaRepository.findById(UUID.fromString(smId)).orElse(null) : null);
                                sec.getSubSections().add(sub);
                            }
                        }
                        blog.getSections().add(sec);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to restore target revision", e);
            throw new BadRequestException("Could not restore target revision: " + e.getMessage());
        }

        return getBlogById(blogRepository.save(blog).getId());
    }

    // ---- Redirects ----

    @Override
    public Map<String, String> getRedirectMap() {
        Map<String, String> map = new HashMap<>();
        for (BlogSlugRedirect r : blogSlugRedirectRepository.findAll()) {
            blogRepository.findById(r.getBlogId()).ifPresent(b -> map.put(r.getOldSlug(), b.getSlug()));
        }
        return map;
    }

    @Override
    public String getRedirectBySlug(String slug) {
        return blogSlugRedirectRepository.findByOldSlug(slug)
                .flatMap(r -> blogRepository.findById(r.getBlogId()).map(Blog::getSlug))
                .orElse(null);
    }

    @Override
    @Transactional
    public void createManualRedirect(String oldSlug, UUID blogId) {
        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new ResourceNotFoundException("Blog", blogId));

        String oldSlugNormalized = oldSlug.trim().toLowerCase().replace(" ", "-");
        if (blog.getSlug().equals(oldSlugNormalized)) {
            throw new BadRequestException("Cannot redirect a slug to itself");
        }

        blogRepository.findBySlug(oldSlugNormalized).ifPresent(active -> {
            throw new BadRequestException("Cannot redirect an active blog slug");
        });

        blogSlugRedirectRepository.findByOldSlug(oldSlugNormalized).ifPresent(blogSlugRedirectRepository::delete);

        BlogSlugRedirect redirect = new BlogSlugRedirect();
        redirect.setOldSlug(oldSlugNormalized);
        redirect.setBlogId(blogId);
        blogSlugRedirectRepository.save(redirect);
    }

    @Override
    @Transactional
    public void deleteManualRedirect(String oldSlug) {
        blogSlugRedirectRepository.findByOldSlug(oldSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Redirect not found: " + oldSlug));
        blogSlugRedirectRepository.deleteByOldSlug(oldSlug);
    }

    // ---- Ratings & Metrics ----

    @Override
    @Transactional
    public BlogResponse rateBlog(UUID blogId, double rating) {
        if (rating < 1.0 || rating > 5.0) {
            throw new BadRequestException("Rating must be between 1 and 5");
        }
        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new ResourceNotFoundException("Blog", blogId));

        int newTotal = blog.getTotalRatings() + 1;
        blog.setAvgRating(Math.round(((blog.getAvgRating() * blog.getTotalRatings()) + rating) / newTotal * 100.0) / 100.0);
        blog.setTotalRatings(newTotal);
        blogRepository.save(blog);

        return getBlogById(blogId);
    }

    @Override
    @Transactional
    public BlogMetricDto incrementMetric(UUID blogId, String metricType) {
        return incrementMetricWithReferrer(blogId, metricType, null);
    }

    public BlogMetricDto incrementMetricWithReferrer(UUID blogId, String metricType, String referrer) {
        BlogMetric metric = blogMetricRepository.findByBlogId(blogId)
                .orElseGet(() -> {
                    BlogMetric m = new BlogMetric();
                    m.setBlogId(blogId);
                    return blogMetricRepository.save(m);
                });

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
        } else if ("like".equalsIgnoreCase(metricType)) {
            metric.setLikes(metric.getLikes() + 1);
        } else if ("read_progress".equalsIgnoreCase(metricType)) {
            metric.setReadProgressCount(metric.getReadProgressCount() + 1);
        } else {
            throw new BadRequestException("Invalid metric type");
        }

        return blogMapper.toDto(blogMetricRepository.save(metric));
    }

    // ---- Internals ----

    private void mapRequestToBlog(BlogRequest request, Blog blog) {
        blog.setTitle(request.getTitle());
        blog.setSlug(request.getSlug());
        blog.setExcerpt(request.getExcerpt());
        blog.setPublishDate(request.getPublishDate());
        blog.setReadTime(request.getReadTime());
        blog.setPublished(request.isPublished());
        blog.setFeatured(request.isFeatured());
        blog.setDisplayOrder(request.getDisplayOrder());

        blog.setCategory(request.getCategoryId() != null ? blogCategoryRepository.findById(request.getCategoryId()).orElse(null) : null);
        blog.setAuthor(request.getAuthorId() != null ? blogAuthorRepository.findById(request.getAuthorId()).orElse(null) : null);
        blog.setBannerImage(request.getBannerImageId() != null ? mediaRepository.findById(request.getBannerImageId()).orElse(null) : null);

        // SEO
        if (request.getSeo() != null) {
            if (blog.getSeo() == null) {
                blog.setSeo(seoMapper.toEntity(request.getSeo()));
            } else {
                SeoMetadata seo = blog.getSeo();
                seo.setMetaTitle(request.getSeo().getMetaTitle());
                seo.setMetaDescription(request.getSeo().getMetaDescription());
                seo.setMetaKeywords(request.getSeo().getMetaKeywords());
                seo.setMetaRobots(request.getSeo().getMetaRobots());
                seo.setMetaImageId(request.getSeo().getMetaImageId());
                seo.setJsonLd(request.getSeo().getJsonLd());
            }
        } else {
            blog.setSeo(null);
        }

        // Tags
        if (request.getTagIds() != null) {
            blog.setTags(tagRepository.findAllById(request.getTagIds()));
        } else {
            blog.getTags().clear();
        }

        // Sections
        blog.getSections().clear();
        if (request.getSections() != null) {
            for (BlogSectionDto secDto : request.getSections()) {
                BlogSection sec = new BlogSection();
                sec.setBlogId(blog.getId());
                sec.setTitle(secDto.getTitle());
                sec.setContent(secDto.getContent());
                sec.setSectionType(secDto.getSectionType());
                sec.setDisplayOrder(secDto.getDisplayOrder());
                sec.setCtaButtonText(secDto.getCtaButtonText());
                sec.setCtaButtonUrl(secDto.getCtaButtonUrl());
                sec.setEmbedUrl(secDto.getEmbedUrl());
                sec.setEmbedProvider(secDto.getEmbedProvider());
                sec.setCodeBlock(secDto.getCodeBlock());
                sec.setCodeLanguage(secDto.getCodeLanguage());

                if (secDto.getMedia() != null && secDto.getMedia().getId() != null) {
                    sec.setMedia(mediaRepository.findById(secDto.getMedia().getId()).orElse(null));
                }

                if (secDto.getSubSections() != null) {
                    for (BlogSubSectionDto subDto : secDto.getSubSections()) {
                        BlogSubSection sub = new BlogSubSection();
                        sub.setTitle(subDto.getTitle());
                        sub.setContent(subDto.getContent());
                        sub.setDisplayOrder(subDto.getDisplayOrder());

                        if (subDto.getMedia() != null && subDto.getMedia().getId() != null) {
                            sub.setMedia(mediaRepository.findById(subDto.getMedia().getId()).orElse(null));
                        }
                        sec.getSubSections().add(sub);
                    }
                }
                blog.getSections().add(sec);
            }
        }
    }

    private Map<String, Object> serializeBlogRevisionData(Blog blog) {
        Map<String, Object> data = new HashMap<>();

        Map<String, Object> settings = new HashMap<>();
        settings.put("slug", blog.getSlug());
        settings.put("is_featured", blog.isFeatured());
        settings.put("is_published", blog.isPublished());
        settings.put("category_id", blog.getCategory() != null ? blog.getCategory().getId().toString() : null);
        settings.put("author_id", blog.getAuthor() != null ? blog.getAuthor().getId().toString() : null);
        settings.put("tag_ids", blog.getTags().stream().map(t -> t.getId().toString()).toList());
        data.put("basic_settings", settings);

        Map<String, Object> seo = new HashMap<>();
        if (blog.getSeo() != null) {
            seo.put("meta_title", blog.getSeo().getMetaTitle());
            seo.put("meta_description", blog.getSeo().getMetaDescription());
            seo.put("meta_keywords", blog.getSeo().getMetaKeywords());
        }
        data.put("seo_data", seo);

        List<Map<String, Object>> sections = new ArrayList<>();
        for (BlogSection s : blog.getSections()) {
            Map<String, Object> sec = new HashMap<>();
            sec.put("title", s.getTitle());
            sec.put("content", s.getContent());
            sec.put("section_type", s.getSectionType());
            sec.put("display_order", s.getDisplayOrder());
            sec.put("media_id", s.getMedia() != null ? s.getMedia().getId().toString() : null);
            sec.put("cta_button_text", s.getCtaButtonText());
            sec.put("cta_button_url", s.getCtaButtonUrl());
            sec.put("embed_url", s.getEmbedUrl());
            sec.put("embed_provider", s.getEmbedProvider());
            sec.put("code_block", s.getCodeBlock());
            sec.put("code_language", s.getCodeLanguage());

            List<Map<String, Object>> subs = new ArrayList<>();
            for (BlogSubSection ss : s.getSubSections()) {
                Map<String, Object> sub = new HashMap<>();
                sub.put("title", ss.getTitle());
                sub.put("content", ss.getContent());
                sub.put("display_order", ss.getDisplayOrder());
                sub.put("media_id", ss.getMedia() != null ? ss.getMedia().getId().toString() : null);
                subs.add(sub);
            }
            sec.put("sub_sections", subs);
            sections.add(sec);
        }
        data.put("sections", sections);

        return data;
    }

    @Override
    public SitemapDataDto getSitemapData() {
        List<SitemapDataDto.SitemapItem> blogs = blogRepository.findByIsPublishedTrue().stream()
                .map(b -> new SitemapDataDto.SitemapItem(b.getSlug(), b.getUpdatedAt()))
                .toList();

        List<SitemapDataDto.SitemapItem> categories = blogCategoryRepository.findAll().stream()
                .map(c -> new SitemapDataDto.SitemapItem(c.getSlug(), c.getUpdatedAt()))
                .toList();

        List<SitemapDataDto.SitemapItem> cmsPages = cmsPageRepository.findAll().stream()
                .filter(CMSPage::isActive)
                .map(p -> new SitemapDataDto.SitemapItem(p.getSlug(), p.getUpdatedAt()))
                .toList();

        return SitemapDataDto.builder()
                .blogs(blogs)
                .categories(categories)
                .cmsPages(cmsPages)
                .build();
    }
}
