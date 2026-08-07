package com.quillforge.api.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "analytics_view_logs", indexes = {
        @Index(name = "idx_analytics_log_entity", columnList = "entity_id, entity_type")
})
@Getter
@Setter
public class AnalyticsViewLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "ip_hash", nullable = false, length = 64)
    private String ipHash;

    @Column(name = "viewed_at", nullable = false)
    private Instant viewedAt = Instant.now();
}
