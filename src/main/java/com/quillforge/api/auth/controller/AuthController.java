package com.quillforge.api.auth.controller;

import com.quillforge.api.common.dto.ApiResponse;
import com.quillforge.api.auth.dto.ChangePasswordRequest;
import com.quillforge.api.user.dto.CreateUserDto;
import com.quillforge.api.auth.dto.ForgotPasswordRequest;
import com.quillforge.api.auth.dto.InviteUserRequest;
import com.quillforge.api.auth.dto.LoginRequest;
import com.quillforge.api.auth.dto.LoginResponseDto;
import com.quillforge.api.auth.dto.RefreshTokenRequest;
import com.quillforge.api.auth.dto.ResetPasswordRequest;
import com.quillforge.api.auth.dto.TokenDto;
import com.quillforge.api.user.dto.UserResponseDto;
import com.quillforge.api.auth.dto.VerifyInvitationRequest;
import com.quillforge.api.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user login, registration, and session management")
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    @Operation(summary = "Register a new admin user", description = "Signs up a new administrative account")
    public ResponseEntity<ApiResponse<UserResponseDto>> register(@RequestBody @Valid CreateUserDto request) {
        UserResponseDto userDto = userService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", userDto));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Validates credentials and returns JWT access and refresh tokens")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(@RequestBody @Valid LoginRequest request) {
        LoginResponseDto response = userService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Refresh access token", description = "Uses a valid refresh token to obtain a new short-lived access token")
    public ResponseEntity<ApiResponse<TokenDto>> refreshToken(@RequestBody @Valid RefreshTokenRequest request) {
        TokenDto tokenDto = userService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", tokenDto));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Revokes the active refresh token session")
    public ResponseEntity<ApiResponse<Void>> logout() {
        userService.logout();
        return ResponseEntity.ok(ApiResponse.success("Logout successful"));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password", description = "Changes password for the authenticated user")
    public ResponseEntity<ApiResponse<Void>> changePassword(@RequestBody @Valid ChangePasswordRequest request) {
        userService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot password", description = "Sends password reset email")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        userService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Password reset email sent"));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Resets password using a token")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        userService.resetPassword(request.getToken(), request.getPassword());
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully"));
    }

    @PostMapping("/invite")
    @Operation(summary = "Invite user", description = "Sends invitation token to a user email")
    public ResponseEntity<ApiResponse<Map<String, Object>>> inviteUser(@RequestBody @Valid InviteUserRequest request) {
        Map<String, Object> result = userService.inviteUser(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Invitation sent", result));
    }

    @PostMapping("/verify-invitation")
    @Operation(summary = "Verify invitation", description = "Verifies invitation token")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyInvitation(@RequestBody @Valid VerifyInvitationRequest request) {
        Map<String, Object> result = userService.verifyInvitation(request.getToken());
        return ResponseEntity.ok(ApiResponse.success("Invitation verified", result));
    }

    @PostMapping("/social/{provider}")
    @Operation(summary = "Social Login", description = "Authenticates via OAuth provider stub")
    public ResponseEntity<ApiResponse<LoginResponseDto>> socialLogin(@PathVariable String provider) {
        LoginResponseDto response = userService.socialLogin(provider);
        return ResponseEntity.ok(ApiResponse.success("Social login successful", response));
    }

    @PostMapping("/social/google")
    @Operation(summary = "Google Login", description = "Authenticates via Google OAuth stub")
    public ResponseEntity<ApiResponse<LoginResponseDto>> googleLogin() {
        LoginResponseDto response = userService.socialLogin("google");
        return ResponseEntity.ok(ApiResponse.success("Google login successful", response));
    }

    @PostMapping("/social/facebook")
    @Operation(summary = "Facebook Login", description = "Authenticates via Facebook OAuth stub")
    public ResponseEntity<ApiResponse<LoginResponseDto>> facebookLogin() {
        LoginResponseDto response = userService.socialLogin("facebook");
        return ResponseEntity.ok(ApiResponse.success("Facebook login successful", response));
    }

    @PostMapping("/social/linkedin")
    @Operation(summary = "Linkedin Login", description = "Authenticates via Linkedin OAuth stub")
    public ResponseEntity<ApiResponse<LoginResponseDto>> linkedinLogin() {
        LoginResponseDto response = userService.socialLogin("linkedin");
        return ResponseEntity.ok(ApiResponse.success("Linkedin login successful", response));
    }

    @PostMapping("/social/apple")
    @Operation(summary = "Apple Login", description = "Authenticates via Apple OAuth stub")
    public ResponseEntity<ApiResponse<LoginResponseDto>> appleLogin() {
        LoginResponseDto response = userService.socialLogin("apple");
        return ResponseEntity.ok(ApiResponse.success("Apple login successful", response));
    }
}
