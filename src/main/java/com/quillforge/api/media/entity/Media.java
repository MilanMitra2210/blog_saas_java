package com.quillforge.api.media.entity;

import org.hibernate.annotations.Filter;

import com.quillforge.api.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Entity
@Table(name = "media")
@SQLRestriction("deleted = false")
@Getter
@Setter
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Media extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(name = "alt_text")
    private String altText;

    @Column(name = "object_key", nullable = false)
    private String key;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private Long size;

    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    @Column(name = "folder_id")
    private UUID folderId;

    private Integer width;

    private Integer height;
}
