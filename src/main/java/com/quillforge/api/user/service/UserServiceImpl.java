package com.quillforge.api.user.service;

import com.quillforge.api.common.dto.PaginatedResponse;
import com.quillforge.api.common.exception.BadRequestException;
import com.quillforge.api.common.exception.ResourceNotFoundException;
import com.quillforge.api.user.dto.ChangePasswordRequest;
import com.quillforge.api.user.dto.CreateUserDto;
import com.quillforge.api.user.dto.LoginRequest;
import com.quillforge.api.user.dto.LoginResponseDto;
import com.quillforge.api.user.dto.TokenDto;
import com.quillforge.api.user.dto.UserResponseDto;
import com.quillforge.api.user.dto.UserUpdateDto;
import com.quillforge.api.user.entity.User;
import com.quillforge.api.user.entity.User.RoleEnum;
import com.quillforge.api.user.entity.User.ProviderEnum;
import com.quillforge.api.user.mapper.UserMapper;
import com.quillforge.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.transaction.annotation.Transactional;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final JavaMailSender mailSender;

    @Override
    public PaginatedResponse<UserResponseDto> getUsers(int page, int limit, String search, RoleEnum role, String status) {
        PageRequest pageRequest = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> userPage = userRepository.findAllFiltered(search, role, status, pageRequest);
        return PaginatedResponse.of(userPage, userMapper::toDto);
    }

    @Override
    public UserResponseDto getMe() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new BadRequestException("Not authenticated");
        }
        String principal = authentication.getName();
        User user;
        try {
            UUID userId = UUID.fromString(principal);
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        } catch (IllegalArgumentException e) {
            user = userRepository.findByEmail(principal)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "email", principal));
        }
        return userMapper.toDto(user);
    }

    @Override
    @Transactional
    public UserResponseDto register(CreateUserDto createDto) {
        userRepository.findDeletedByEmail(createDto.getEmail()).ifPresent(deletedUser -> {
            userRepository.hardDeleteById(deletedUser.getId());
            userRepository.flush();
        });

        if (userRepository.existsActiveByEmail(createDto.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        User user = userMapper.toEntity(createDto);
        user.setPassword(passwordEncoder.encode(createDto.getPassword()));
        // Default to USER role for sign-ups
        user.setRole(RoleEnum.USER);
        user.setProvider(ProviderEnum.MANUAL);
        User saved = userRepository.save(user);
        return userMapper.toDto(saved);
    }

    @Override
    @Transactional
    public UserResponseDto createUser(CreateUserDto createDto) {
        userRepository.findDeletedByEmail(createDto.getEmail()).ifPresent(deletedUser -> {
            userRepository.hardDeleteById(deletedUser.getId());
            userRepository.flush();
        });

        if (userRepository.existsActiveByEmail(createDto.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        User user = userMapper.toEntity(createDto);
        user.setPassword(passwordEncoder.encode(createDto.getPassword()));
        
        if (createDto.getRole() != null) {
            user.setRole(createDto.getRole());
        } else {
            user.setRole(RoleEnum.USER);
        }
        
        user.setProvider(ProviderEnum.MANUAL);
        user.setActive(createDto.isActive());
        user.setImageId(createDto.getImageId());
        user.setRoleId(createDto.getRoleId());

        User saved = userRepository.save(user);
        return userMapper.toDto(saved);
    }

    @Override
    @Transactional
    public LoginResponseDto login(LoginRequest loginRequest) {
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid credentials"));

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new BadRequestException("Invalid credentials");
        }

        if (!user.isActive()) {
            throw new BadRequestException("User account is inactive");
        }

        if (user.isBlocked()) {
            throw new BadRequestException("User account has been blocked: " + user.getBlockReason());
        }

        String accessToken = tokenService.generateAccessToken(user);
        String refreshToken = tokenService.generateRefreshToken(user);

        user.setRefreshToken(refreshToken);
        userRepository.save(user);

        return LoginResponseDto.builder()
                .user(userMapper.toDto(user))
                .tokens(TokenDto.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .build())
                .build();
    }

    @Override
    @Transactional
    public TokenDto refreshToken(String refreshToken) {
        if (!tokenService.validateToken(refreshToken)) {
            throw new BadRequestException("Invalid refresh token");
        }

        String email = tokenService.getEmailFromToken(refreshToken)
                .orElseThrow(() -> new BadRequestException("Invalid refresh token claims"));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (!refreshToken.equals(user.getRefreshToken())) {
            throw new BadRequestException("Token mismatch or revoked");
        }

        String newAccessToken = tokenService.generateAccessToken(user);
        String newRefreshToken = tokenService.generateRefreshToken(user);

        user.setRefreshToken(newRefreshToken);
        userRepository.save(user);

        return TokenDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    @Override
    @Transactional
    public void logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return;
        }
        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null) {
            user.setRefreshToken("");
            userRepository.save(user);
        }
    }

    @Override
    @Cacheable(value = "users", key = "#id")
    public UserResponseDto getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        return userMapper.toDto(user);
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", key = "#id")
    public UserResponseDto updateUser(UUID id, UserUpdateDto updateDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        userMapper.updateEntityFromDto(updateDto, user);
        User savedUser = userRepository.save(user);
        return userMapper.toDto(savedUser);
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", key = "#id")
    public void deleteUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        user.setDeleted(true);
        user.setDeletedAt(Instant.now());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new BadRequestException("Not authenticated");
        }
        String principal = authentication.getName();
        User user;
        try {
            UUID userId = UUID.fromString(principal);
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        } catch (IllegalArgumentException e) {
            user = userRepository.findByEmail(principal)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "email", principal));
        }

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BadRequestException("Invalid current password");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BadRequestException("New password cannot be the same as the current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        String resetToken = tokenService.generateAccessToken(user);
        String link = "http://localhost:5174/reset-password?code=" + resetToken + "&type=reset-password";
        
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("QuillForge Password Reset");
        message.setText("Click the following link to reset your password: " + link);
        mailSender.send(message);
        
        System.out.println("🚀 [EMAIL SENT] Click here to reset your password: " + link);
    }

    @Override
    @Transactional
    public void resetPassword(String token, String password) {
        if (!tokenService.validateToken(token)) {
            throw new BadRequestException("Invalid or expired password reset token");
        }
        String email = tokenService.getEmailFromToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid token claims"));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
    }

    @Override
    public Map<String, Object> inviteUser(String email) {
        String inviteToken = Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(Keys.hmacShaKeyFor("dGhpcy1pcy1hLXNlY3JldC1rZXktZm9yLXF1aWxsZm9yZ2Utc3ByaW5nLWJvb3QtYmFja2VuZC1kZXZlbG9wbWVudC11c2U=".getBytes(StandardCharsets.UTF_8)))
                .compact();

        String link = "http://localhost:5174/reset-password?code=" + inviteToken + "&type=invite";
        
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("QuillForge Invitation");
        message.setText("Click the following link to accept your invitation: " + link);
        mailSender.send(message);

        System.out.println("🚀 [EMAIL SENT] Click here to accept your invitation: " + link);
        return Map.of("success", true, "token", inviteToken);
    }

    @Override
    public Map<String, Object> verifyInvitation(String token) {
        try {
            String email = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor("dGhpcy1pcy1hLXNlY3JldC1rZXktZm9yLXF1aWxsZm9yZ2Utc3ByaW5nLWJvb3QtYmFja2VuZC1kZXZlbG9wbWVudC11c2U=".getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
            return Map.of("success", true, "email", email);
        } catch (Exception e) {
            throw new BadRequestException("Invalid or expired invitation token");
        }
    }

    @Override
    @Transactional
    public LoginResponseDto socialLogin(String provider) {
        String email = provider + "-user@quillforge.com";
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setName(provider.substring(0, 1).toUpperCase() + provider.substring(1) + " User");
                    newUser.setEmail(email);
                    newUser.setRole(RoleEnum.USER);
                    newUser.setActive(true);
                    newUser.setProvider(ProviderEnum.valueOf(provider.toUpperCase()));
                    return userRepository.save(newUser);
                });

        String accessToken = tokenService.generateAccessToken(user);
        String refreshToken = tokenService.generateRefreshToken(user);
        user.setRefreshToken(refreshToken);
        userRepository.save(user);

        return LoginResponseDto.builder()
                .user(userMapper.toDto(user))
                .tokens(TokenDto.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .build())
                .build();
    }
}
