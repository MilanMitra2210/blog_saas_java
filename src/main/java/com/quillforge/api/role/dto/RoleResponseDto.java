package com.quillforge.api.role.dto;

import com.quillforge.api.user.dto.UserAuditDto;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class RoleResponseDto {

    private UUID id;
    private String name;
    private String description;
    private String[] permissions;
    private Instant createdAt;
    private Instant updatedAt;
    private UserAuditDto createdBy;
    private UserAuditDto updatedBy;
}
