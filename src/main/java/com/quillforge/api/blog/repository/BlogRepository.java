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

    @Query("SELECT b FROM Blog b WHERE " +
           "(:search IS NULL OR :search = '' OR LOWER(b.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(b.excerpt) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:isPublished IS NULL OR b.isPublished = :isPublished) AND " +
           "(:categoryId IS NULL OR b.category.id = :categoryId)")
    Page<Blog> findAllFiltered(
            @Param("search") String search,
            @Param("isPublished") Boolean isPublished,
            @Param("categoryId") UUID categoryId,
            Pageable pageable
    );

    java.util.List<Blog> findByIsPublishedTrue();
}
