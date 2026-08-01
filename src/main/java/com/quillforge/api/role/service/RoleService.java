package com.quillforge.api.role.service;

import com.quillforge.api.role.dto.CreateRoleDto;
import com.quillforge.api.role.dto.PermissionDto;
import com.quillforge.api.role.dto.RoleResponseDto;
import com.quillforge.api.role.dto.UpdateRoleDto;

import java.util.List;
import java.util.UUID;

public interface RoleService {

    List<RoleResponseDto> getAllRoles();

    RoleResponseDto getRoleById(UUID id);

    RoleResponseDto createRole(CreateRoleDto createDto);

    RoleResponseDto updateRole(UUID id, UpdateRoleDto updateDto);

    void deleteRole(UUID id);

    List<PermissionDto> getAvailablePermissions();
}
