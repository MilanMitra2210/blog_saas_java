package com.quillforge.api.role.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateRoleDto {

    private String name;

    private String description;

    private String[] permissions;
}
