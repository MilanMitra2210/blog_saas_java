package com.quillforge.api.user.mapper;

import com.quillforge.api.media.mapper.MediaMapper;
import com.quillforge.api.role.dto.RoleResponseDto;
import com.quillforge.api.role.entity.Role;
import com.quillforge.api.user.dto.CreateUserDto;
import com.quillforge.api.user.dto.UserAuditDto;
import com.quillforge.api.user.dto.UserResponseDto;
import com.quillforge.api.user.dto.UserUpdateDto;
import com.quillforge.api.user.entity.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", uses = {MediaMapper.class})
public interface UserMapper {

    @Mapping(source = "createdBy", target = "createdBy", qualifiedByName = "toAuditDto")
    @Mapping(source = "updatedBy", target = "updatedBy", qualifiedByName = "toAuditDto")
    @Mapping(source = "roleRel", target = "roleRel", qualifiedByName = "toRoleDto")
    UserResponseDto toDto(User user);

    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "roleRel", ignore = true)
    User toEntity(CreateUserDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "roleRel", ignore = true)
    void updateEntityFromDto(UserUpdateDto dto, @MappingTarget User user);

    @Named("toAuditDto")
    default UserAuditDto toAuditDto(User user) {
        if (user == null) return null;
        UserAuditDto dto = new UserAuditDto();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setRole(user.getRole() != null ? user.getRole().name() : null);
        return dto;
    }

    @Named("toRoleDto")
    default RoleResponseDto toRoleDto(Role role) {
        if (role == null) return null;
        RoleResponseDto dto = new RoleResponseDto();
        dto.setId(role.getId());
        dto.setName(role.getName());
        dto.setDescription(role.getDescription());
        dto.setPermissions(role.getPermissions());
        return dto;
    }
}
