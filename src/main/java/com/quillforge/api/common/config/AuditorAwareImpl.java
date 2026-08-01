package com.quillforge.api.common.config;

import com.quillforge.api.user.entity.User;
import com.quillforge.api.user.repository.UserRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.AuditorAware;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Provides the current auditor (User entity) for JPA auditing.
 * Uses @Lazy on UserRepository to avoid circular dependency during startup.
 */
@Component("auditorAware")
public class AuditorAwareImpl implements AuditorAware<User> {

    private final UserRepository userRepository;

    public AuditorAwareImpl(@Lazy UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @NonNull
    public Optional<User> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.empty();
        }

        String principal = authentication.getName();

        // Try UUID lookup first (normal authenticated requests)
        try {
            UUID userId = UUID.fromString(principal);
            return userRepository.findById(userId);
        } catch (IllegalArgumentException e) {
            // Fallback: principal is an email (legacy tokens)
            return userRepository.findByEmail(principal);
        }
    }
}
