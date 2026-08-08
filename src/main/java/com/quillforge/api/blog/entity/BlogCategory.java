package com.quillforge.api.blog.entity;

import org.hibernate.annotations.Filter;

import com.quillforge.api.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "blog_categories")
@SQLRestriction("deleted = false")
@Getter
@Setter
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class BlogCategory extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}
