package com.quillforge.api.tenant;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;

/**
 * Registry service to programmatically construct and register Hikari connection pools for dedicated databases.
 */
@Component
@Slf4j
public class TenantDataSourceRegistry {

    private final TenantRoutingDataSource routingDataSource;

    @Value("${spring.datasource.url}")
    private String defaultUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.driver-class-name:org.postgresql.Driver}")
    private String driverClassName;

    public TenantDataSourceRegistry(@Lazy DataSource dataSource) {
        if (dataSource instanceof org.springframework.aop.framework.Advised advised) {
            try {
                this.routingDataSource = (TenantRoutingDataSource) advised.getTargetSource().getTarget();
            } catch (Exception e) {
                throw new IllegalStateException("Failed to unwrap DataSource proxy", e);
            }
        } else {
            this.routingDataSource = (TenantRoutingDataSource) dataSource;
        }
    }

    /**
     * Dynamically registers a database connection pool for a specific tenant.
     * @return The registered DataSource instance
     */
    public DataSource createAndRegisterPool(String tenantId) {
        if (routingDataSource.hasTenantDataSource(tenantId)) {
            // Retrieve from routingDataSource target mapping if already exists
            return routingDataSource.getTenantDataSource(tenantId);
        }

        log.info("Constructing dedicated connection pool for tenant: {}", tenantId);

        // Derive JDBC URL: replace base database name with tenant-specific name
        String baseUri = defaultUrl.substring(0, defaultUrl.lastIndexOf("/") + 1);
        String tenantDbUrl = baseUri + "quillforge_" + tenantId.replace("-", "_");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(tenantDbUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);
        config.setMaximumPoolSize(5); // Restrict connection limits per dedicated pool
        config.setMinimumIdle(1);
        config.setPoolName("TenantDB-Pool-" + tenantId);

        try {
            HikariDataSource ds = new HikariDataSource(config);
            routingDataSource.registerTenantDataSource(tenantId, ds);
            log.info("Successfully registered connection pool for tenant: {}", tenantId);
            return ds;
        } catch (Exception e) {
            log.error("Failed to construct connection pool for tenant: {}", tenantId, e);
            throw new RuntimeException("Failed to register tenant connection pool", e);
        }
    }
}
