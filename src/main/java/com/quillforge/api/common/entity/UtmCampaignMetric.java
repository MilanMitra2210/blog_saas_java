package com.quillforge.api.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "utm_campaign_metrics", indexes = {
        @Index(name = "idx_utm_camp_entity_utm", columnList = "entity_id, entity_type, utm_source, utm_medium, utm_campaign", unique = true)
})
@Getter
@Setter
public class UtmCampaignMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "utm_source", nullable = true, length = 100)
    private String utmSource;

    @Column(name = "utm_medium", nullable = true, length = 100)
    private String utmMedium;

    @Column(name = "utm_campaign", nullable = true, length = 100)
    private String utmCampaign;

    @Column(nullable = false)
    private int views = 0;

    @Column(name = "unique_views", nullable = false)
    private int uniqueViews = 0;
}
