package com.quillforge.api.blog.repository;

import com.quillforge.api.blog.entity.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TagRepository extends JpaRepository<Tag, UUID> {
    Optional<Tag> findBySlug(String slug);
    boolean existsBySlug(String slug);

    @Query("SELECT t FROM Tag t WHERE " +
           "(:search IS NULL OR :search = '' OR LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(t.slug) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Tag> findAllFiltered(@Param("search") String search, Pageable pageable);
}
