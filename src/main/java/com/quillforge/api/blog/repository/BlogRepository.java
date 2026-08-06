package com.quillforge.api.blog.repository;

import com.quillforge.api.blog.entity.Blog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface BlogRepository extends JpaRepository<Blog, UUID> {
    Optional<Blog> findBySlug(String slug);
    boolean existsBySlug(String slug);

    @Query("SELECT DISTINCT b FROM Blog b LEFT JOIN b.tags t WHERE " +
           "(:search IS NULL OR :search = '' OR LOWER(b.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(b.excerpt) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:isPublished IS NULL OR b.isPublished = :isPublished) AND " +
           "(:categoryId IS NULL OR b.category.id = :categoryId) AND " +
           "(:categorySlug IS NULL OR :categorySlug = '' OR b.category.slug = :categorySlug) AND " +
           "(:tagId IS NULL OR t.id = :tagId) AND " +
           "(:tagSlug IS NULL OR :tagSlug = '' OR LOWER(t.slug) = LOWER(:tagSlug) OR LOWER(t.name) = LOWER(:tagSlug))")
    Page<Blog> findAllFiltered(
            @Param("search") String search,
            @Param("isPublished") Boolean isPublished,
            @Param("categoryId") UUID categoryId,
            @Param("categorySlug") String categorySlug,
            @Param("tagId") UUID tagId,
            @Param("tagSlug") String tagSlug,
            Pageable pageable
    );

    java.util.List<Blog> findByIsPublishedTrue();
}
