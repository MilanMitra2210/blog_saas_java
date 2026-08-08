package com.quillforge.api.tenant;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;

@Service
@Slf4j
public class TenantMigrationService {

    private final HikariDataSource defaultDataSource;
    private final TenantDataSourceRegistry dataSourceRegistry;

    public TenantMigrationService(
            @Qualifier("defaultDataSource") HikariDataSource defaultDataSource,
            TenantDataSourceRegistry dataSourceRegistry) {
        this.defaultDataSource = defaultDataSource;
        this.dataSourceRegistry = dataSourceRegistry;
    }

    /**
     * Entry point to migrate a tenant's data to an isolated database.
     */
    public void migrateTenantToDedicatedDatabase(String tenantId) {
        String dbSuffix = tenantId.replace("-", "_").toLowerCase();
        String dbName = "quillforge_" + dbSuffix;

        try {
            log.info("Starting database migration for tenant: {}", tenantId);
            
            // 1. Create the new physical database schema
            createDatabase(dbName);

            // 2. Register connection pool inside our routing registry
            DataSource tenantDataSource = dataSourceRegistry.createAndRegisterPool(tenantId);

            // 3. Bootstrap database schema tables dynamically
            bootstrapTenantSchema(tenantDataSource);

            // 4. Copy tenant data table by table in dependency order
            copyTenantData(tenantId, defaultDataSource, tenantDataSource);

            // 5. Purge tenant data from the shared database
            purgeTenantData(tenantId, defaultDataSource);

            log.info("Database migration completed successfully for tenant: {}", tenantId);
            
        } catch (Exception e) {
            log.error("Migration failed for tenant: {}", tenantId, e);
            throw new RuntimeException("Tenant database migration failed", e);
        }
    }

    private void bootstrapTenantSchema(DataSource tenantDataSource) {
        log.info("Bootstrapping JPA schema on the dedicated database...");
        
        org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean emf = 
            new org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(tenantDataSource);
        emf.setPackagesToScan("com.quillforge.api");
        
        org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter vendorAdapter = 
            new org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter();
        emf.setJpaVendorAdapter(vendorAdapter);
        
        java.util.Properties jpaProperties = new java.util.Properties();
        jpaProperties.put("hibernate.hbm2ddl.auto", "update");
        jpaProperties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        jpaProperties.put("hibernate.show_sql", "false");
        jpaProperties.put("hibernate.format_sql", "false");
        emf.setJpaProperties(jpaProperties);
        
        emf.afterPropertiesSet(); // Triggers schema creation on the tenant database
        emf.destroy();            // Close Hibernate bootstrap factory immediately
        
        log.info("Dedicated database schema bootstrapped successfully");
    }

    private void createDatabase(String dbName) throws Exception {
        log.info("Creating dedicated PostgreSQL database: {}", dbName);
        
        try (Connection conn = defaultDataSource.getConnection()) {
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(true);
            
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SELECT 1 FROM pg_database WHERE datname = '" + dbName + "'");
                var rs = stmt.getResultSet();
                if (!rs.next()) {
                    stmt.executeUpdate("CREATE DATABASE " + dbName);
                    log.info("Database {} created successfully", dbName);
                } else {
                    log.info("Database {} already exists, skipping creation", dbName);
                }
            } finally {
                conn.setAutoCommit(autoCommit);
            }
        }
    }

    private void copyTenantData(String tenantId, DataSource sourceDs, DataSource targetDs) throws Exception {
        log.info("Executing transactional table copy pipeline for tenant: {}", tenantId);

        // Copy seo_metadata first (independent but referenced by blogs and pages)
        copySeoMetadata(tenantId, sourceDs, targetDs);

        // Copy standard isolated tables in order of foreign key dependencies
        String[] tablesInOrder = {
            "media",
            "company_settings",
            "blog_categories",
            "blog_tags",
            "blog_authors",
            "users",
            "blogs",
            "cms_pages",
            "blog_comments",
            "analytics_metrics"
        };

        for (String table : tablesInOrder) {
            copyTableData(table, tenantId, sourceDs, targetDs);
        }

        // Copy join/association tables
        copyBlogPostTags(tenantId, sourceDs, targetDs);
    }

    private void copyTableData(String tableName, String tenantId, DataSource sourceDs, DataSource targetDs) throws Exception {
        log.info("Copying table: {}", tableName);
        String selectSql = "SELECT * FROM " + tableName + " WHERE tenant_id = ?";
        
        try (Connection sourceConn = sourceDs.getConnection();
             PreparedStatement selectStmt = sourceConn.prepareStatement(selectSql)) {
            
            selectStmt.setString(1, tenantId);
            try (ResultSet rs = selectStmt.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                
                StringBuilder insertSql = new StringBuilder("INSERT INTO " + tableName + " (");
                StringBuilder valuesPlaceholders = new StringBuilder(" VALUES (");
                for (int i = 1; i <= columnCount; i++) {
                    insertSql.append(metaData.getColumnName(i));
                    valuesPlaceholders.append("?");
                    if (i < columnCount) {
                        insertSql.append(", ");
                        valuesPlaceholders.append(", ");
                    }
                }
                insertSql.append(")").append(valuesPlaceholders).append(")");
                
                try (Connection targetConn = targetDs.getConnection();
                     PreparedStatement insertStmt = targetConn.prepareStatement(insertSql.toString())) {
                    
                    int batchSize = 0;
                    while (rs.next()) {
                        for (int i = 1; i <= columnCount; i++) {
                            insertStmt.setObject(i, rs.getObject(i));
                        }
                        insertStmt.addBatch();
                        batchSize++;
                        
                        if (batchSize % 100 == 0) {
                            insertStmt.executeBatch();
                        }
                    }
                    if (batchSize % 100 != 0) {
                        insertStmt.executeBatch();
                    }
                    log.info("Successfully copied {} rows for table {}", batchSize, tableName);
                }
            }
        }
    }

    private void copySeoMetadata(String tenantId, DataSource sourceDs, DataSource targetDs) throws Exception {
        log.info("Copying seo_metadata for tenant: {}", tenantId);
        String selectSql = "SELECT * FROM seo_metadata WHERE id IN (SELECT seo_id FROM blogs WHERE tenant_id = ?) " +
                           "OR id IN (SELECT seo_id FROM cms_pages WHERE tenant_id = ?)";
        
        try (Connection sourceConn = sourceDs.getConnection();
             PreparedStatement selectStmt = sourceConn.prepareStatement(selectSql)) {
            
            selectStmt.setString(1, tenantId);
            selectStmt.setString(2, tenantId);
            
            try (ResultSet rs = selectStmt.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                
                StringBuilder insertSql = new StringBuilder("INSERT INTO seo_metadata (");
                StringBuilder valuesPlaceholders = new StringBuilder(" VALUES (");
                for (int i = 1; i <= columnCount; i++) {
                    insertSql.append(metaData.getColumnName(i));
                    valuesPlaceholders.append("?");
                    if (i < columnCount) {
                        insertSql.append(", ");
                        valuesPlaceholders.append(", ");
                    }
                }
                insertSql.append(")").append(valuesPlaceholders).append(")");
                
                try (Connection targetConn = targetDs.getConnection();
                     PreparedStatement insertStmt = targetConn.prepareStatement(insertSql.toString())) {
                    
                    int count = 0;
                    while (rs.next()) {
                        for (int i = 1; i <= columnCount; i++) {
                            insertStmt.setObject(i, rs.getObject(i));
                        }
                        insertStmt.addBatch();
                        count++;
                    }
                    if (count > 0) {
                        insertStmt.executeBatch();
                    }
                    log.info("Successfully copied {} rows for seo_metadata", count);
                }
            }
        }
    }

    private void copyBlogPostTags(String tenantId, DataSource sourceDs, DataSource targetDs) throws Exception {
        log.info("Copying blog_post_tags mapping for tenant: {}", tenantId);
        String selectSql = "SELECT * FROM blog_post_tags WHERE post_id IN (SELECT id FROM blogs WHERE tenant_id = ?)";
        
        try (Connection sourceConn = sourceDs.getConnection();
             PreparedStatement selectStmt = sourceConn.prepareStatement(selectSql)) {
            
            selectStmt.setString(1, tenantId);
            
            try (ResultSet rs = selectStmt.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                
                StringBuilder insertSql = new StringBuilder("INSERT INTO blog_post_tags (");
                StringBuilder valuesPlaceholders = new StringBuilder(" VALUES (");
                for (int i = 1; i <= columnCount; i++) {
                    insertSql.append(metaData.getColumnName(i));
                    valuesPlaceholders.append("?");
                    if (i < columnCount) {
                        insertSql.append(", ");
                        valuesPlaceholders.append(", ");
                    }
                }
                insertSql.append(")").append(valuesPlaceholders).append(")");
                
                try (Connection targetConn = targetDs.getConnection();
                     PreparedStatement insertStmt = targetConn.prepareStatement(insertSql.toString())) {
                    
                    int count = 0;
                    while (rs.next()) {
                        for (int i = 1; i <= columnCount; i++) {
                            insertStmt.setObject(i, rs.getObject(i));
                        }
                        insertStmt.addBatch();
                        count++;
                    }
                    if (count > 0) {
                        insertStmt.executeBatch();
                    }
                    log.info("Successfully copied {} rows for blog_post_tags", count);
                }
            }
        }
    }

    private void purgeTenantData(String tenantId, DataSource sourceDs) throws Exception {
        log.info("Purging tenant {} data from the shared database...", tenantId);
        
        // Remove mappings first, then delete tables in reverse dependency order
        String[] deleteQueries = {
            "DELETE FROM blog_post_tags WHERE post_id IN (SELECT id FROM blogs WHERE tenant_id = ?)",
            "DELETE FROM blog_comments WHERE tenant_id = ?",
            "DELETE FROM blogs WHERE tenant_id = ?",
            "DELETE FROM cms_pages WHERE tenant_id = ?",
            "DELETE FROM users WHERE tenant_id = ?",
            "DELETE FROM blog_authors WHERE tenant_id = ?",
            "DELETE FROM blog_tags WHERE tenant_id = ?",
            "DELETE FROM blog_categories WHERE tenant_id = ?",
            "DELETE FROM company_settings WHERE tenant_id = ?",
            "DELETE FROM media WHERE tenant_id = ?",
            "DELETE FROM analytics_metrics WHERE entity_id NOT IN (SELECT id FROM blogs) AND entity_id NOT IN (SELECT id FROM cms_pages)",
            "DELETE FROM seo_metadata WHERE id NOT IN (SELECT seo_id FROM blogs) AND id NOT IN (SELECT seo_id FROM cms_pages)"
        };

        try (Connection conn = sourceDs.getConnection()) {
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            
            try {
                for (String sql : deleteQueries) {
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        // Bind either 1 or 2 parameters depending on query structure
                        int paramCount = stmt.getParameterMetaData().getParameterCount();
                        for (int i = 1; i <= paramCount; i++) {
                            stmt.setString(i, tenantId);
                        }
                        int affectedRows = stmt.executeUpdate();
                        log.info("Executing purge: {} -> affected {} rows", sql, affectedRows);
                    }
                }
                conn.commit();
                log.info("Shared data purge successfully committed for tenant {}", tenantId);
            } catch (Exception e) {
                conn.rollback();
                log.error("Purging failed, rolled back changes for tenant {}", tenantId, e);
                throw e;
            } finally {
                conn.setAutoCommit(autoCommit);
            }
        }
    }
}
