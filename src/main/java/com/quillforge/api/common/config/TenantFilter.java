package com.quillforge.api.common.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import java.io.IOException;

/**
 * Filter to resolve tenant ID from headers or host subdomains for incoming requests.
 */
@Component
public class TenantFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String tenantId = null;

        // 1. Check custom X-Tenant-ID header
        tenantId = httpRequest.getHeader("X-Tenant-ID");

        // 2. Fallback: Parse subdomain from Host header
        if (tenantId == null || tenantId.trim().isEmpty()) {
            String host = httpRequest.getHeader("Host");
            if (host != null) {
                if (host.contains(":")) {
                    host = host.substring(0, host.indexOf(":"));
                }
                String[] parts = host.split("\\.");
                if (parts.length > 2) {
                    String subdomain = parts[0];
                    // Skip system subdomains
                    if (!"www".equalsIgnoreCase(subdomain) && 
                        !"admin".equalsIgnoreCase(subdomain) && 
                        !"api".equalsIgnoreCase(subdomain)) {
                        tenantId = subdomain;
                    }
                }
            }
        }

        // 3. Fallback to default tenant
        if (tenantId == null || tenantId.trim().isEmpty()) {
            tenantId = TenantContext.DEFAULT_TENANT;
        }

        TenantContext.setCurrentTenant(tenantId);
        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
