package com.quillforge.api.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.quillforge.api.role.dto.RoleResponseDto;
import com.quillforge.api.user.entity.User.RoleEnum;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class UserResponseDto {

    private UUID id;
    private String name;
    private String email;
    private RoleEnum role;
    private RoleResponseDto roleRel;
    private boolean active;
    private boolean blocked;
    private String blockReason;
    private String provider;
    private UUID imageId;

    @JsonProperty("createdAt")
    private Instant createdAt;

    @JsonProperty("updatedAt")
    private Instant updatedAt;

    @JsonProperty("createdBy")
    private UserAuditDto createdBy;

    @JsonProperty("updatedBy")
    private UserAuditDto updatedBy;
}
