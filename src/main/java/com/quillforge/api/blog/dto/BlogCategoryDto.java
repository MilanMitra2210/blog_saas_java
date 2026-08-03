package com.quillforge.api.blog.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.quillforge.api.user.dto.UserAuditDto;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class BlogCategoryDto {
    private UUID id;
    private String name;
    private String slug;

    @JsonProperty("isActive")
    private boolean isActive;

    private Instant createdAt;
    private Instant updatedAt;

    @JsonProperty("createdBy")
    private UserAuditDto createdBy;

    @JsonProperty("updatedBy")
    private UserAuditDto updatedBy;
}
