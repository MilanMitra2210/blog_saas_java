package com.quillforge.api.user.service;

import com.quillforge.api.common.dto.PaginatedResponse;
import com.quillforge.api.user.dto.ChangePasswordRequest;
import com.quillforge.api.user.dto.CreateUserDto;
import com.quillforge.api.user.dto.LoginRequest;
import com.quillforge.api.user.dto.LoginResponseDto;
import com.quillforge.api.user.dto.TokenDto;
import com.quillforge.api.user.dto.UserResponseDto;
import com.quillforge.api.user.dto.UserUpdateDto;
import com.quillforge.api.user.entity.User.RoleEnum;

import java.util.Map;
import java.util.UUID;

public interface UserService {

    PaginatedResponse<UserResponseDto> getUsers(int page, int limit, String search, RoleEnum role, String status);

    UserResponseDto getMe();

    UserResponseDto getUserById(UUID id);

    UserResponseDto updateUser(UUID id, UserUpdateDto updateDto);

    UserResponseDto register(CreateUserDto createDto);

    UserResponseDto createUser(CreateUserDto createDto);

    LoginResponseDto login(LoginRequest loginRequest);

    TokenDto refreshToken(String refreshToken);

    void logout();

    void changePassword(ChangePasswordRequest request);

    void forgotPassword(String email);

    void resetPassword(String token, String password);

    Map<String, Object> inviteUser(String email);

    Map<String, Object> verifyInvitation(String token);

    LoginResponseDto socialLogin(String provider);

    void deleteUser(UUID id);
}
