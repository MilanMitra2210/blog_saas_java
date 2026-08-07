package com.quillforge.api.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "analytics_metrics", indexes = {
        @Index(name = "idx_analytics_entity", columnList = "entity_id, entity_type", unique = true)
})
@Getter
@Setter
public class AnalyticsMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(nullable = false)
    private int views = 0;

    @Column(nullable = false)
    private int likes = 0;

    @Column(name = "read_progress_count", nullable = false)
    private int readProgressCount = 0;

    @Column(name = "google_views", nullable = false)
    private int googleViews = 0;

    @Column(name = "twitter_views", nullable = false)
    private int twitterViews = 0;

    @Column(name = "linkedin_views", nullable = false)
    private int linkedinViews = 0;

    @Column(name = "direct_views", nullable = false)
    private int directViews = 0;

    @Column(name = "other_views", nullable = false)
    private int otherViews = 0;

    @Column(name = "unique_views", nullable = false)
    private int uniqueViews = 0;

    @Column(name = "mobile_views", nullable = false)
    private int mobileViews = 0;

    @Column(name = "tablet_views", nullable = false)
    private int tabletViews = 0;

    @Column(name = "desktop_views", nullable = false)
    private int desktopViews = 0;

    public double getCompletionRate() {
        return views > 0 ? Math.round((double) readProgressCount / views * 100.0 * 10.0) / 10.0 : 0.0;
    }
}
