package com.quillforge.api.user.mapper;

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

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(source = "createdBy", target = "createdBy", qualifiedByName = "toAuditDto")
    @Mapping(source = "updatedBy", target = "updatedBy", qualifiedByName = "toAuditDto")
    UserResponseDto toDto(User user);

    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    User toEntity(CreateUserDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
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
}
