package com.quillforge.api.user.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class UserAuditDto {
    private UUID id;
    private String name;
    private String role;
}
