package com.quillforge.api.blog.entity;

import org.hibernate.annotations.Filter;

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
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
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
    @NotFound(action = NotFoundAction.IGNORE)
    private BlogCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    @NotFound(action = NotFoundAction.IGNORE)
    private BlogAuthor author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banner_image_id")
    @NotFound(action = NotFoundAction.IGNORE)
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

    @PrePersist
    @PreUpdate
    public void calculateReadTime() {
        StringBuilder textBuilder = new StringBuilder();
        if (title != null) textBuilder.append(title).append(" ");
        if (excerpt != null) textBuilder.append(excerpt).append(" ");

        if (sections != null) {
            for (BlogSection sec : sections) {
                if (sec.getTitle() != null) textBuilder.append(sec.getTitle()).append(" ");
                if (sec.getContent() != null) textBuilder.append(sec.getContent().toString()).append(" ");
                if (sec.getSubSections() != null) {
                    for (BlogSubSection sub : sec.getSubSections()) {
                        if (sub.getTitle() != null) textBuilder.append(sub.getTitle()).append(" ");
                        if (sub.getContent() != null) textBuilder.append(sub.getContent().toString()).append(" ");
                    }
                }
            }
        }

        String fullText = textBuilder.toString().replaceAll("<[^>]*>", " ");
        String[] words = fullText.trim().split("\\s+");
        int wordCount = words.length == 1 && words[0].isEmpty() ? 0 : words.length;

        // Average reading speed: 200 words per minute
        this.readTime = Math.max(1, (int) Math.ceil(wordCount / 200.0));
    }
}
