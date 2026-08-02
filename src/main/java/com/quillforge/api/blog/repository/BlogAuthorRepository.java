package com.quillforge.api.blog.repository;

import com.quillforge.api.blog.entity.BlogAuthor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface BlogAuthorRepository extends JpaRepository<BlogAuthor, UUID> {
    @Query("SELECT a FROM BlogAuthor a WHERE " +
           "(:search IS NULL OR :search = '' OR LOWER(a.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(a.designation) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<BlogAuthor> findAllFiltered(@Param("search") String search, Pageable pageable);
}
