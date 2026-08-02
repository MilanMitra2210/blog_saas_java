package com.quillforge.api.blog.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "blog_slug_redirects")
@Getter
@Setter
public class BlogSlugRedirect {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @Column(name = "old_slug", nullable = false, unique = true, length = 500)
    private String oldSlug;

    @Column(name = "blog_id", nullable = false)
    private UUID blogId;
}
