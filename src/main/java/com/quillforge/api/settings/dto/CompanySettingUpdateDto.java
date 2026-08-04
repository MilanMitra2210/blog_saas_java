package com.quillforge.api.settings.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompanySettingUpdateDto {
    private String companyName;
    private String email;
    private String whatsappNumber;
    private String whatsappMessage;
    private String registeredOfficeAddress;
    private String manufacturingPlantAddress;
    private String cin;
    private String mapUrl;

    // Social urls
    private String facebookUrl;
    private String instagramUrl;
    private String pinterestUrl;
    private String linkedinUrl;
    private String twitterUrl;
    private String youtubeUrl;
    private String githubUrl;
    private String tiktokUrl;
}
