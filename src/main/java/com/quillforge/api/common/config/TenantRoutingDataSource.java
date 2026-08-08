package com.quillforge.api.common.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dynamic routing datasource that switches connection pools at runtime based on the active TenantContext.
 */
public class TenantRoutingDataSource extends AbstractRoutingDataSource {

    private final Map<Object, Object> dataSources = new ConcurrentHashMap<>();

    public TenantRoutingDataSource(HikariDataSource defaultDataSource) {
        setDefaultTargetDataSource(defaultDataSource);
        dataSources.put(TenantContext.DEFAULT_TENANT, defaultDataSource);
        setTargetDataSources(dataSources);
    }

    @Override
    protected Object determineCurrentLookupKey() {
        return TenantContext.getCurrentTenant();
    }

    /**
     * Dynamically registers a connection pool for a newly upgraded/migrated tenant.
     */
    public void registerTenantDataSource(String tenantId, javax.sql.DataSource dataSource) {
        dataSources.put(tenantId, dataSource);
        setTargetDataSources(dataSources);
        afterPropertiesSet(); // Force Spring to rebuild the lookup mapping
    }

    public boolean hasTenantDataSource(String tenantId) {
        return dataSources.containsKey(tenantId);
    }
}
