package com.quillforge.api.common.config;

import org.springframework.data.domain.AuditorAware;
import org.springframework.lang.NonNull;

import java.util.Optional;

/**
 * Provides the current auditor (user identity) for JPA auditing.
 */
public class AuditorAwareImpl implements AuditorAware<String> {

    @Override
    @NonNull
    public Optional<String> getCurrentAuditor() {
        // TODO: Replace with SecurityContext lookup when Auth module is built
        return Optional.of("system");
    }
}
