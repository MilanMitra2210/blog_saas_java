package com.quillforge.api.blog.repository;

import com.quillforge.api.blog.entity.BlogViewLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BlogViewLogRepository extends JpaRepository<BlogViewLog, UUID> {
    boolean existsByBlogIdAndIpHash(UUID blogId, String ipHash);
}
