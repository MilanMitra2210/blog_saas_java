package com.quillforge.api.user.service;

import com.quillforge.api.common.dto.PaginatedResponse;
import com.quillforge.api.user.dto.UserResponseDto;
import com.quillforge.api.user.dto.UserUpdateDto;
import com.quillforge.api.user.entity.User.RoleEnum;

import java.util.UUID;

public interface UserService {

    PaginatedResponse<UserResponseDto> getUsers(int page, int limit, String search, RoleEnum role, String status);

    UserResponseDto getMe();

    UserResponseDto getUserById(UUID id);

    UserResponseDto updateUser(UUID id, UserUpdateDto updateDto);

    void deleteUser(UUID id);
}
