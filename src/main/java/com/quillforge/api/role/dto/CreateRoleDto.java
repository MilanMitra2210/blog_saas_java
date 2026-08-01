package com.quillforge.api.role.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateRoleDto {

    @NotBlank(message = "Role name is required")
    private String name;

    private String description;

    private String[] permissions = {};
}
