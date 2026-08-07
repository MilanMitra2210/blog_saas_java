package com.quillforge.api.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "engagement_milestones", indexes = {
        @Index(name = "idx_engagement_entity", columnList = "entity_id, entity_type", unique = true)
})
@Getter
@Setter
public class EngagementMilestone {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "milestone_25", nullable = false)
    private int milestone25 = 0;

    @Column(name = "milestone_50", nullable = false)
    private int milestone50 = 0;

    @Column(name = "milestone_75", nullable = false)
    private int milestone75 = 0;

    @Column(name = "milestone_100", nullable = false)
    private int milestone100 = 0;
}
