package com.quillforge.api.user.mapper;

import com.quillforge.api.user.dto.UserResponseDto;
import com.quillforge.api.user.dto.UserUpdateDto;
import com.quillforge.api.user.entity.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponseDto toDto(User user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(UserUpdateDto dto, @MappingTarget User user);
}
