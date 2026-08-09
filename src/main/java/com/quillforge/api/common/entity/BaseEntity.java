package com.quillforge.api.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.quillforge.api.user.entity.User;
import com.quillforge.api.tenant.TenantContext;

import java.time.Instant;
import java.util.UUID;

/**
 * Base entity providing common fields for all domain entities in QuillForge.
 */
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = String.class))
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", updatable = false)
    @CreatedBy
    @NotFound(action = NotFoundAction.IGNORE)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    @LastModifiedBy
    @NotFound(action = NotFoundAction.IGNORE)
    private User updatedBy;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(nullable = false)
    private boolean deleted = false;

    private Instant deletedAt;

    @Column(name = "tenant_id", nullable = false, length = 64)
    @ColumnDefault("'default'")
    private String tenantId = "default";

    @jakarta.persistence.PrePersist
    public void populateTenantId() {
        String currentTenant = TenantContext.getCurrentTenant();
        if (currentTenant != null && !currentTenant.trim().isEmpty()) {
            if (this.tenantId == null || "default".equals(this.tenantId)) {
                this.tenantId = currentTenant;
            }
        }
    }
}
