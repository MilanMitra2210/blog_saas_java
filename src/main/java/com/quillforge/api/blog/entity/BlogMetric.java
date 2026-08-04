package com.quillforge.api.blog.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "blog_metrics")
@Getter
@Setter
public class BlogMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @Column(name = "blog_id", nullable = false, unique = true)
    private UUID blogId;

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
}
