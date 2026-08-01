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

    UserResponseDto register(com.quillforge.api.user.dto.CreateUserDto createDto);

    com.quillforge.api.user.dto.LoginResponseDto login(com.quillforge.api.user.dto.LoginRequest loginRequest);

    com.quillforge.api.user.dto.TokenDto refreshToken(String refreshToken);

    void logout();

    void changePassword(com.quillforge.api.user.dto.ChangePasswordRequest request);

    void forgotPassword(String email);

    void resetPassword(String token, String password);

    java.util.Map<String, Object> inviteUser(String email);

    java.util.Map<String, Object> verifyInvitation(String token);

    com.quillforge.api.user.dto.LoginResponseDto socialLogin(String provider);

    void deleteUser(UUID id);
}
