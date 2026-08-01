package com.quillforge.api.role.mapper;

import com.quillforge.api.role.dto.CreateRoleDto;
import com.quillforge.api.role.dto.RoleResponseDto;
import com.quillforge.api.role.dto.UpdateRoleDto;
import com.quillforge.api.role.entity.Role;
import com.quillforge.api.user.dto.UserAuditDto;
import com.quillforge.api.user.entity.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    @Mapping(source = "createdBy", target = "createdBy", qualifiedByName = "toAuditDto")
    @Mapping(source = "updatedBy", target = "updatedBy", qualifiedByName = "toAuditDto")
    RoleResponseDto toDto(Role role);

    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Role toEntity(CreateRoleDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(UpdateRoleDto dto, @MappingTarget Role role);

    @Named("toAuditDto")
    default UserAuditDto toAuditDto(User user) {
        if (user == null) return null;
        UserAuditDto dto = new UserAuditDto();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setRole(user.getRole() != null ? user.getRole().name() : null);
        return dto;
    }
}
