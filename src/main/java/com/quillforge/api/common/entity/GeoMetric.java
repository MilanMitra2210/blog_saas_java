package com.quillforge.api.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "geo_metrics", indexes = {
        @Index(name = "idx_geo_entity_country", columnList = "entity_id, entity_type, country_code", unique = true)
})
@Getter
@Setter
public class GeoMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(nullable = false)
    private int views = 0;

    @Column(name = "unique_views", nullable = false)
    private int uniqueViews = 0;
}
