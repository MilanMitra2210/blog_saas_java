package com.quillforge.api.user.dto;

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
    private UUID roleId;
    private boolean active;
    private boolean blocked;
    private String blockReason;
    private String provider;
    private UUID imageId;
    private Instant createdAt;
    private Instant updatedAt;
    private UserAuditDto createdBy;
    private UserAuditDto updatedBy;
}
