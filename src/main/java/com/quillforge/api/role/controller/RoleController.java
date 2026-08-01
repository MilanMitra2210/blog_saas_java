package com.quillforge.api.role.controller;

import com.quillforge.api.common.dto.ApiResponse;
import com.quillforge.api.role.dto.CreateRoleDto;
import com.quillforge.api.role.dto.PermissionDto;
import com.quillforge.api.role.dto.RoleResponseDto;
import com.quillforge.api.role.dto.UpdateRoleDto;
import com.quillforge.api.role.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/roles")
@RequiredArgsConstructor
@Tag(name = "Roles", description = "Endpoints for managing roles and permissions")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @Operation(summary = "Get all roles", description = "Retrieves all non-deleted roles")
    public ResponseEntity<ApiResponse<List<RoleResponseDto>>> getAllRoles() {
        List<RoleResponseDto> roles = roleService.getAllRoles();
        return ResponseEntity.ok(ApiResponse.success("Roles retrieved", roles));
    }

    @GetMapping("/permissions")
    @Operation(summary = "Get available permissions", description = "Returns the full list of assignable permission definitions")
    public ResponseEntity<ApiResponse<List<PermissionDto>>> getAvailablePermissions() {
        List<PermissionDto> permissions = roleService.getAvailablePermissions();
        return ResponseEntity.ok(ApiResponse.success("Permissions retrieved", permissions));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get role by ID", description = "Retrieves a single role by its UUID")
    public ResponseEntity<ApiResponse<RoleResponseDto>> getRoleById(@PathVariable UUID id) {
        RoleResponseDto role = roleService.getRoleById(id);
        return ResponseEntity.ok(ApiResponse.success("Role retrieved", role));
    }

    @PostMapping({"", "/"})
    @Operation(summary = "Create role", description = "Creates a new role with permissions")
    public ResponseEntity<ApiResponse<RoleResponseDto>> createRole(@Valid @RequestBody CreateRoleDto createDto) {
        RoleResponseDto role = roleService.createRole(createDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Role created successfully", role));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update role", description = "Updates an existing role")
    public ResponseEntity<ApiResponse<RoleResponseDto>> updateRole(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRoleDto updateDto
    ) {
        RoleResponseDto role = roleService.updateRole(id, updateDto);
        return ResponseEntity.ok(ApiResponse.success("Role updated successfully", role));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete role", description = "Soft-deletes a role")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable UUID id) {
        roleService.deleteRole(id);
        return ResponseEntity.ok(ApiResponse.success("Role deleted successfully"));
    }
}
