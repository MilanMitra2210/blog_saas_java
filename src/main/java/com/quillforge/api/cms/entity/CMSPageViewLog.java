package com.quillforge.api.cms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cms_page_view_logs", indexes = {
        @Index(name = "idx_cms_page_view_logs_page_ip", columnList = "page_id, ip_hash", unique = true)
})
@Getter
@Setter
public class CMSPageViewLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @Column(name = "page_id", nullable = false)
    private UUID pageId;

    @Column(name = "ip_hash", nullable = false, length = 64)
    private String ipHash;

    @Column(name = "viewed_at", nullable = false)
    private Instant viewedAt = Instant.now();
}
