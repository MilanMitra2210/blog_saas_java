package com.quillforge.api.blog.entity;

import com.quillforge.api.media.entity.Media;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "blog_sections")
@Getter
@Setter
public class BlogSection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @Column(name = "blog_id")
    private UUID blogId;

    private String title;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Object content;

    @Column(name = "section_type")
    private String sectionType = "content";

    @Column(name = "display_order")
    private int displayOrder = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_id")
    private Media media;

    @Column(name = "cta_button_text")
    private String ctaButtonText;

    @Column(name = "cta_button_url")
    private String ctaButtonUrl;

    @Column(name = "embed_url")
    private String embedUrl;

    @Column(name = "embed_provider")
    private String embedProvider;

    @Column(name = "code_block", columnDefinition = "TEXT")
    private String codeBlock;

    @Column(name = "code_language")
    private String codeLanguage;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id")
    @OrderBy("displayOrder ASC")
    private List<BlogSubSection> subSections = new ArrayList<>();
}
