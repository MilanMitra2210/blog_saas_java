package com.quillforge.api.cms.entity;

import com.quillforge.api.common.entity.BaseEntity;
import com.quillforge.api.common.entity.ContentBlock;
import com.quillforge.api.common.entity.SeoMetadata;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "cms_pages")
@SQLRestriction("deleted = false")
@Getter
@Setter
public class CMSPage extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "seo_id")
    @NotFound(action = NotFoundAction.IGNORE)
    private SeoMetadata seo;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id")
    private List<ContentBlock> contentBlocks = new ArrayList<>();
}
