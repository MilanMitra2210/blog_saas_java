package com.quillforge.api.tenant;

import jakarta.persistence.EntityManager;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.security.core.Authentication;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Aspect to automatically enable the Hibernate filter "tenantFilter" before executing repository methods.
 */
@Aspect
@Component
@Slf4j
public class TenantFilterAspect {

    @PersistenceContext
    private EntityManager entityManager;

    @Before("execution(* org.springframework.data.repository.Repository+.*(..))")
    public void enableTenantFilter() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"))) {
            return;
        }

        Session session = entityManager.unwrap(Session.class);
        if (session != null) {
            String currentTenant = TenantContext.getCurrentTenant();
            session.enableFilter("tenantFilter").setParameter("tenantId", currentTenant);
        }
    }
}
