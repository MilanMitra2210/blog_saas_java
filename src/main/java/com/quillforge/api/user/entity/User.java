package com.quillforge.api.user.entity;

import org.hibernate.annotations.Filter;

import com.quillforge.api.common.entity.BaseEntity;
import com.quillforge.api.media.entity.Media;
import com.quillforge.api.role.entity.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Entity
@Table(name = "users")
@SQLRestriction("deleted = false")
@Getter
@Setter
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class User extends BaseEntity {

    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    private String password;

    @Column(nullable = false)
    private boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleEnum role = RoleEnum.USER;

    @Column(name = "refresh_token")
    private String refreshToken = "";

    @Column(nullable = false)
    private boolean blocked = false;

    @Column(name = "block_reason")
    private String blockReason = "";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProviderEnum provider = ProviderEnum.MANUAL;

    @Column(name = "facebook_id")
    private String facebookId;

    @Column(name = "linkedin_id")
    private String linkedinId;

    @Column(name = "image_id")
    private UUID imageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id", insertable = false, updatable = false)
    private Media image;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role roleRel;

    public enum RoleEnum {
        USER, ADMIN, SUPER_ADMIN
    }

    public enum ProviderEnum {
        MANUAL, GOOGLE, FACEBOOK, LINKEDIN, APPLE
    }
}
