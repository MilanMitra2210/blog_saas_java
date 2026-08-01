package com.quillforge.api.role.service;

import com.quillforge.api.common.exception.BadRequestException;
import com.quillforge.api.common.exception.ResourceNotFoundException;
import com.quillforge.api.role.dto.CreateRoleDto;
import com.quillforge.api.role.dto.PermissionDto;
import com.quillforge.api.role.dto.RoleResponseDto;
import com.quillforge.api.role.dto.UpdateRoleDto;
import com.quillforge.api.role.entity.Role;
import com.quillforge.api.role.mapper.RoleMapper;
import com.quillforge.api.role.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    /**
     * Static list of all available permissions in the system.
     * Grouped by feature area for the admin UI.
     */
    private static final List<PermissionDto> AVAILABLE_PERMISSIONS = List.of(
            // Users
            new PermissionDto("users:read", "View Users", "Users"),
            new PermissionDto("users:write", "Create / Edit Users", "Users"),
            // CMS Pages
            new PermissionDto("cms_pages:read", "View CMS Pages", "CMS Pages"),
            new PermissionDto("cms_pages:write", "Create / Edit CMS Pages", "CMS Pages"),
            // Blogs
            new PermissionDto("blogs:read", "View Blog Posts", "Blogs"),
            new PermissionDto("blogs:write", "Create / Edit Blog Posts", "Blogs"),
            // Media
            new PermissionDto("media:read", "View Media", "Media"),
            new PermissionDto("media:write", "Upload Media", "Media"),
            // Enquiries
            new PermissionDto("enquiries:read", "View Enquiries", "Enquiries"),
            new PermissionDto("enquiries:write", "Respond to Enquiries", "Enquiries"),
            // Company Settings
            new PermissionDto("company_settings:read", "View Settings", "Settings"),
            new PermissionDto("company_settings:write", "Modify Settings", "Settings")
    );

    @Override
    public List<RoleResponseDto> getAllRoles() {
        return roleRepository.findAll()
                .stream()
                .map(roleMapper::toDto)
                .toList();
    }

    @Override
    public RoleResponseDto getRoleById(UUID id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", id));
        return roleMapper.toDto(role);
    }

    @Override
    @Transactional
    public RoleResponseDto createRole(CreateRoleDto createDto) {
        if (roleRepository.existsByName(createDto.getName())) {
            throw new BadRequestException("Role with name '" + createDto.getName() + "' already exists");
        }

        Role role = roleMapper.toEntity(createDto);
        Role saved = roleRepository.save(role);
        return roleMapper.toDto(saved);
    }

    @Override
    @Transactional
    public RoleResponseDto updateRole(UUID id, UpdateRoleDto updateDto) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", id));

        // Check unique name if it changed
        if (updateDto.getName() != null
                && !updateDto.getName().equals(role.getName())
                && roleRepository.existsByName(updateDto.getName())) {
            throw new BadRequestException("Role with name '" + updateDto.getName() + "' already exists");
        }

        roleMapper.updateEntityFromDto(updateDto, role);
        Role saved = roleRepository.save(role);
        return roleMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void deleteRole(UUID id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", id));
        role.setDeleted(true);
        role.setDeletedAt(Instant.now());
        roleRepository.save(role);
    }

    @Override
    public List<PermissionDto> getAvailablePermissions() {
        return AVAILABLE_PERMISSIONS;
    }
}
