package com.quillforge.api.blog.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "blog_view_logs", indexes = {
        @Index(name = "idx_blog_view_logs_blog_ip", columnList = "blog_id, ip_hash", unique = true)
})
@Getter
@Setter
public class BlogViewLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @Column(name = "blog_id", nullable = false)
    private UUID blogId;

    @Column(name = "ip_hash", nullable = false, length = 64)
    private String ipHash;

    @Column(name = "viewed_at", nullable = false)
    private Instant viewedAt = Instant.now();
}
