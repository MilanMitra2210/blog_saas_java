package com.quillforge.api.user.service;

import com.quillforge.api.common.dto.PaginatedResponse;
import com.quillforge.api.common.exception.ResourceNotFoundException;
import com.quillforge.api.user.dto.UserResponseDto;
import com.quillforge.api.user.dto.UserUpdateDto;
import com.quillforge.api.user.entity.User;
import com.quillforge.api.user.entity.User.RoleEnum;
import com.quillforge.api.user.entity.User.ProviderEnum;
import com.quillforge.api.user.mapper.UserMapper;
import com.quillforge.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public PaginatedResponse<UserResponseDto> getUsers(int page, int limit, String search, RoleEnum role, String status) {
        PageRequest pageRequest = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> userPage = userRepository.findAllFiltered(search, role, status, pageRequest);
        return PaginatedResponse.of(userPage, userMapper::toDto);
    }

    @Override
    @Transactional
    public UserResponseDto getMe() {
        // Fallback mock / first user lookup since security isn't wired yet
        User user = userRepository.findByEmail("admin@quillforge.com")
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setName("Admin User");
                    newUser.setEmail("admin@quillforge.com");
                    newUser.setRole(RoleEnum.ADMIN);
                    newUser.setActive(true);
                    newUser.setProvider(ProviderEnum.MANUAL);
                    return userRepository.save(newUser);
                });
        return userMapper.toDto(user);
    }

    @Override
    public UserResponseDto getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        return userMapper.toDto(user);
    }

    @Override
    @Transactional
    public UserResponseDto updateUser(UUID id, UserUpdateDto updateDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        userMapper.updateEntityFromDto(updateDto, user);
        User savedUser = userRepository.save(user);
        return userMapper.toDto(savedUser);
    }

    @Override
    @Transactional
    public void deleteUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        user.setDeleted(true);
        user.setDeletedAt(java.time.Instant.now());
        userRepository.save(user);
    }
}
