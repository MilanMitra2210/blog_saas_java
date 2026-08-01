package com.quillforge.api.user.controller;

import com.quillforge.api.common.dto.ApiResponse;
import com.quillforge.api.common.dto.PaginatedResponse;
import com.quillforge.api.user.dto.UserResponseDto;
import com.quillforge.api.user.dto.UserUpdateDto;
import com.quillforge.api.user.entity.User.RoleEnum;
import com.quillforge.api.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Endpoints for managing user accounts and retrieving profiles")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Retrieves details of the authenticated user")
    public ResponseEntity<ApiResponse<UserResponseDto>> getMyProfile() {
        UserResponseDto userDto = userService.getMe();
        return ResponseEntity.ok(ApiResponse.success("User info retrieved", userDto));
    }

    @GetMapping
    @Operation(summary = "Get all users", description = "Retrieves paginated and filtered list of users")
    public ResponseEntity<ApiResponse<PaginatedResponse<UserResponseDto>>> getAllUsers(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "role", required = false) RoleEnum role,
            @RequestParam(value = "status", required = false) String status
    ) {
        PaginatedResponse<UserResponseDto> paginatedResponse = userService.getUsers(page, limit, search, role, status);
        return ResponseEntity.ok(ApiResponse.success("Users retrieved", paginatedResponse));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Retrieves a user by their UUID")
    public ResponseEntity<ApiResponse<UserResponseDto>> getUserById(@PathVariable UUID id) {
        UserResponseDto userDto = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("User retrieved", userDto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user", description = "Updates details of a user account")
    public ResponseEntity<ApiResponse<UserResponseDto>> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UserUpdateDto updateDto
    ) {
        UserResponseDto userDto = userService.updateUser(id, updateDto);
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", userDto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user", description = "Soft-deletes a user account")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully"));
    }
}
