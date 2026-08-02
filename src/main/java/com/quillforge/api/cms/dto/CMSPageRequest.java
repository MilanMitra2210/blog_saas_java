package com.quillforge.api.cms.dto;

import com.quillforge.api.common.dto.ContentBlockDto;
import com.quillforge.api.common.dto.SeoDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * DTO for creating or updating a CMS Page.
 */
@Getter
@Setter
public class CMSPageRequest {

    @NotBlank(message = "Page name is required")
    private String name;

    @NotBlank(message = "Page slug is required")
    private String slug;

    @JsonProperty("isActive")
    private boolean isActive = true;

    @Valid
    private SeoDto seo;

    @JsonProperty("contentBlocks")
    private List<ContentBlockDto> contentBlocks;
}
