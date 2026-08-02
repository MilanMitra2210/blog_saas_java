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
}
