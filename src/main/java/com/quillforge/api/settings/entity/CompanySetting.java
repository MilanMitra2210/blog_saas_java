package com.quillforge.api.settings.entity;

import com.quillforge.api.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "company_settings")
@SQLRestriction("deleted = false")
@Getter
@Setter
public class CompanySetting extends BaseEntity {

    @Column(name = "company_name", length = 255)
    private String companyName;

    @Column(length = 255)
    private String email;

    @Column(name = "whatsapp_number", length = 50)
    private String whatsappNumber;

    @Column(name = "whatsapp_message", columnDefinition = "TEXT")
    private String whatsappMessage;

    @Column(name = "registered_office_address", columnDefinition = "TEXT")
    private String registeredOfficeAddress;

    @Column(name = "manufacturing_plant_address", columnDefinition = "TEXT")
    private String manufacturingPlantAddress;

    @Column(length = 100)
    private String cin;

    @Column(name = "map_url", length = 1000)
    private String mapUrl;

    @Column(name = "facebook_url", length = 500)
    private String facebookUrl;

    @Column(name = "instagram_url", length = 500)
    private String instagramUrl;

    @Column(name = "pinterest_url", length = 500)
    private String pinterestUrl;

    @Column(name = "linkedin_url", length = 500)
    private String linkedinUrl;

    @Column(name = "twitter_url", length = 500)
    private String twitterUrl;

    @Column(name = "youtube_url", length = 500)
    private String youtubeUrl;

    @Column(name = "github_url", length = 500)
    private String githubUrl;

    @Column(name = "tiktok_url", length = 500)
    private String tiktokUrl;
}
