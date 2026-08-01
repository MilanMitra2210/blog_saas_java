package com.quillforge.api.role.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class PermissionDto {

    private String key;
    private String label;
    private String group;
}
