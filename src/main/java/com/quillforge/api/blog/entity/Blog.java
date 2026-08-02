package com.quillforge.api.blog.entity;

import com.quillforge.api.common.entity.BaseEntity;
import com.quillforge.api.common.entity.SeoMetadata;
import com.quillforge.api.media.entity.Media;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "blogs")
@SQLRestriction("deleted = false")
@Getter
@Setter
public class Blog extends BaseEntity {

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, unique = true, length = 500)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String excerpt;

    @Column(name = "publish_date")
    private Instant publishDate;

    @Column(name = "read_time")
    private int readTime = 5;

    @Column(name = "is_published")
    private boolean isPublished = false;

    @Column(name = "is_featured")
    private boolean isFeatured = false;

    @Column(name = "display_order")
    private int displayOrder = 0;

    @Column(name = "avg_rating")
    private double avgRating = 0.0;

    @Column(name = "total_ratings")
    private int totalRatings = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private BlogCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private BlogAuthor author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banner_image_id")
    private Media bannerImage;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "seo_id")
    @NotFound(action = NotFoundAction.IGNORE)
    private SeoMetadata seo;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "blog_id")
    @OrderBy("displayOrder ASC")
    private List<BlogSection> sections = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "blog_tags_association",
            joinColumns = @JoinColumn(name = "blog_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private List<Tag> tags = new ArrayList<>();
}
