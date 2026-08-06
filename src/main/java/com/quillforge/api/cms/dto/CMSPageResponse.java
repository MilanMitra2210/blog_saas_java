package com.quillforge.api.cms.dto;

import com.quillforge.api.common.dto.ContentBlockDto;
import com.quillforge.api.common.dto.SeoDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.quillforge.api.user.dto.UserAuditDto;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO representing a CMS Page details returned to client.
 */
@Getter
@Setter
public class CMSPageResponse {

    private UUID id;
    private String name;
    private String slug;

    @JsonProperty("isActive")
    private boolean isActive;

    private SeoDto seo;

    @JsonProperty("contentBlocks")
    private List<ContentBlockDto> contentBlocks;

    @JsonProperty("createdAt")
    private Instant createdAt;

    @JsonProperty("updatedAt")
    private Instant updatedAt;

    @JsonProperty("createdBy")
    private UserAuditDto createdBy;

    @JsonProperty("updatedBy")
    private UserAuditDto updatedBy;

    @JsonProperty("metrics")
    private CMSPageMetricDto metrics;
}
