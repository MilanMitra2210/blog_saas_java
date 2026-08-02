package com.quillforge.api.blog.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "blog_revisions")
@Getter
@Setter
public class BlogRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @Column(name = "blog_id", nullable = false)
    private UUID blogId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String excerpt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sections_data", columnDefinition = "jsonb")
    private Object sectionsData;

    @Column(name = "revision_number", nullable = false)
    private int revisionNumber = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
