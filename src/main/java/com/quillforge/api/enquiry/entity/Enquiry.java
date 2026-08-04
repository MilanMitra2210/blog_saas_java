package com.quillforge.api.enquiry.entity;

import com.quillforge.api.common.entity.BaseEntity;
import com.quillforge.api.media.entity.Media;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "enquiries")
@SQLRestriction("deleted = false")
@Getter
@Setter
public class Enquiry extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String type;

    @Column(length = 255)
    private String name;

    @Column(length = 255)
    private String email;

    @Column(length = 100)
    private String phone;

    @Column(length = 255)
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false, length = 50)
    private String status = "pending";

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attachment_id")
    private Media attachment;

    @Column(columnDefinition = "TEXT")
    private String details;
}
