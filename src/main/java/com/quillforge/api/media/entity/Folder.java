package com.quillforge.api.media.entity;

import com.quillforge.api.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Entity
@Table(name = "folders")
@SQLRestriction("deleted = false")
@Getter
@Setter
public class Folder extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(name = "parent_id")
    private UUID parentId;
}
