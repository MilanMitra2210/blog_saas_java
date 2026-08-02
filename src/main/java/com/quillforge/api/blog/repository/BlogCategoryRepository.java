package com.quillforge.api.blog.repository;

import com.quillforge.api.blog.entity.BlogCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface BlogCategoryRepository extends JpaRepository<BlogCategory, UUID> {
    Optional<BlogCategory> findBySlug(String slug);
    boolean existsBySlug(String slug);

    @Query("SELECT c FROM BlogCategory c WHERE " +
           "(:search IS NULL OR :search = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(c.slug) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<BlogCategory> findAllFiltered(@Param("search") String search, Pageable pageable);
}
