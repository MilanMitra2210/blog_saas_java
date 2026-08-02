package com.quillforge.api.blog.repository;

import com.quillforge.api.blog.entity.BlogMetric;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BlogMetricRepository extends JpaRepository<BlogMetric, UUID> {
    Optional<BlogMetric> findByBlogId(UUID blogId);
}
