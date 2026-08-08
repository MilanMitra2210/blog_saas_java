package com.quillforge.api.common.config;

import jakarta.persistence.EntityManager;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

/**
 * Aspect to automatically enable the Hibernate filter "tenantFilter" before executing repository methods.
 */
@Aspect
@Component
public class TenantFilterAspect {

    @PersistenceContext
    private EntityManager entityManager;

    @Before("execution(* org.springframework.data.repository.Repository+.*(..))")
    public void enableTenantFilter() {
        Session session = entityManager.unwrap(Session.class);
        if (session != null) {
            String currentTenant = TenantContext.getCurrentTenant();
            session.enableFilter("tenantFilter").setParameter("tenantId", currentTenant);
        }
    }
}
