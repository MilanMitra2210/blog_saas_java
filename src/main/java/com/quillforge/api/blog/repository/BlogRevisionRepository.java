package com.quillforge.api.blog.repository;

import com.quillforge.api.blog.entity.BlogRevision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BlogRevisionRepository extends JpaRepository<BlogRevision, UUID> {
    List<BlogRevision> findByBlogIdOrderByRevisionNumberDesc(UUID blogId);
    long countByBlogId(UUID blogId);
}
