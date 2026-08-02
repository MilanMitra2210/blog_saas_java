package com.quillforge.api.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.quillforge.api.media.dto.MediaDto;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Common DTO representing search engine optimization (SEO) metadata.
 */
@Getter
@Setter
public class SeoDto {
    @JsonProperty("metaTitle")
    private String metaTitle;

    @JsonProperty("metaDescription")
    private String metaDescription;

    @JsonProperty("metaKeywords")
    private String metaKeywords;

    @JsonProperty("metaRobots")
    private String metaRobots;

    @JsonProperty("metaImageId")
    private UUID metaImageId;

    @JsonProperty("metaImage")
    private MediaDto metaImage;

    @JsonProperty("jsonLd")
    private List<Map<String, Object>> jsonLd;
}
