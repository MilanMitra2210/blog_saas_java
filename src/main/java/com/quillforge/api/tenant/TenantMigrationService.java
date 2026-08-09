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
        String dbSuffix = tenantId.replace("-", "_").replace(".", "_").toLowerCase();
        String dbName = "quillforge_" + dbSuffix;

        try {
            log.info("Starting database migration for tenant: {}", tenantId);
            
            // 1. Create the new physical database schema
            createDatabase(dbName);

            // 2. Register connection pool inside our routing registry
            DataSource tenantDataSource = dataSourceRegistry.createAndRegisterPool(tenantId);

            // 3. Bootstrap database schema tables dynamically
            bootstrapTenantSchema(tenantDataSource);

            // 4. Temporarily override TenantContext to "default" to ensure source reads hit the shared database
            String originalTenant = TenantContext.getCurrentTenant();
            TenantContext.setCurrentTenant(TenantContext.DEFAULT_TENANT);
            try {
                // Copy tenant data table by table in dependency order
                copyTenantData(tenantId, defaultDataSource, tenantDataSource);

                // Purge tenant data from the shared database
                purgeTenantData(tenantId, defaultDataSource);
            } finally {
                TenantContext.setCurrentTenant(originalTenant);
            }

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
        jpaProperties.put("hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");
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

        // 1. Clean target tables first in reverse foreign key order to prevent duplicate keys on retries
        String[] tablesInReverse = {
            "blog_tags_association",
            "enquiries",
            "utm_campaign_metrics",
            "engagement_milestones",
            "analytics_view_logs",
            "analytics_metrics",
            "content_blocks",
            "blog_slug_redirects",
            "blog_sub_sections",
            "blog_sections",
            "blog_revisions",
            "blog_comments",
            "cms_pages",
            "blogs",
            "users",
            "blog_authors",
            "blog_tags",
            "blog_categories",
            "company_settings",
            "media",
            "folders",
            "roles",
            "seo_metadata"
        };
        try (Connection targetConn = targetDs.getConnection();
             Statement stmt = targetConn.createStatement()) {
            for (String table : tablesInReverse) {
                stmt.executeUpdate("DELETE FROM " + table);
            }
            log.info("Cleaned target tables in dedicated database successfully");
        }

        // Copy seo_metadata first (independent but referenced by blogs and pages)
        copySeoMetadata(tenantId, sourceDs, targetDs);

        // Copy standard isolated tables in order of foreign key dependencies
        String[] tablesInOrder = {
            "roles",
            "folders",
            "media",
            "company_settings",
            "blog_categories",
            "blog_tags",
            "blog_authors",
            "users",
            "blogs",
            "cms_pages",
            "blog_comments",
            "blog_revisions",
            "blog_sections",
            "blog_sub_sections",
            "blog_slug_redirects",
            "content_blocks",
            "analytics_metrics",
            "analytics_view_logs",
            "engagement_milestones",
            "utm_campaign_metrics",
            "enquiries"
        };

        for (String table : tablesInOrder) {
            copyTableData(table, tenantId, sourceDs, targetDs);
        }

        // Copy join/association tables
        copyBlogTagsAssociation(tenantId, sourceDs, targetDs);
    }

    private void copyTableData(String tableName, String tenantId, DataSource sourceDs, DataSource targetDs) throws Exception {
        log.info("Copying table: {}", tableName);
        String selectSql = getSelectSqlForTable(tableName);
        
        try (Connection sourceConn = sourceDs.getConnection();
             PreparedStatement selectStmt = sourceConn.prepareStatement(selectSql)) {
            
            int paramCount = selectSql.length() - selectSql.replace("?", "").length();
            for (int i = 1; i <= paramCount; i++) {
                selectStmt.setString(i, tenantId);
            }
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

    private void copyBlogTagsAssociation(String tenantId, DataSource sourceDs, DataSource targetDs) throws Exception {
        log.info("Copying blog_tags_association mapping for tenant: {}", tenantId);
        String selectSql = "SELECT * FROM blog_tags_association WHERE blog_id IN (SELECT id FROM blogs WHERE tenant_id = ?)";
        
        try (Connection sourceConn = sourceDs.getConnection();
             PreparedStatement selectStmt = sourceConn.prepareStatement(selectSql)) {
            
            selectStmt.setString(1, tenantId);
            
            try (ResultSet rs = selectStmt.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                
                StringBuilder insertSql = new StringBuilder("INSERT INTO blog_tags_association (");
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
                    log.info("Successfully copied {} rows for blog_tags_association", count);
                }
            }
        }
    }

    private void purgeTenantData(String tenantId, DataSource sourceDs) throws Exception {
        log.info("Purging tenant {} data from the shared database...", tenantId);
        
        // Remove mappings first, then delete tables in reverse dependency order
        String[] deleteQueries = {
            "DELETE FROM blog_tags_association WHERE blog_id IN (SELECT id FROM blogs WHERE tenant_id = ?)",
            "DELETE FROM utm_campaign_metrics WHERE entity_id IN (SELECT id FROM blogs WHERE tenant_id = ?) OR entity_id IN (SELECT id FROM cms_pages WHERE tenant_id = ?)",
            "DELETE FROM engagement_milestones WHERE entity_id IN (SELECT id FROM blogs WHERE tenant_id = ?) OR entity_id IN (SELECT id FROM cms_pages WHERE tenant_id = ?)",
            "DELETE FROM analytics_view_logs WHERE entity_id IN (SELECT id FROM blogs WHERE tenant_id = ?) OR entity_id IN (SELECT id FROM cms_pages WHERE tenant_id = ?)",
            "DELETE FROM analytics_metrics WHERE entity_id IN (SELECT id FROM blogs WHERE tenant_id = ?) OR entity_id IN (SELECT id FROM cms_pages WHERE tenant_id = ?)",
            "DELETE FROM content_blocks WHERE page_id IN (SELECT id FROM cms_pages WHERE tenant_id = ?)",
            "DELETE FROM blog_slug_redirects WHERE blog_id IN (SELECT id FROM blogs WHERE tenant_id = ?)",
            "DELETE FROM blog_sub_sections WHERE section_id IN (SELECT id FROM blog_sections WHERE blog_id IN (SELECT id FROM blogs WHERE tenant_id = ?))",
            "DELETE FROM blog_sections WHERE blog_id IN (SELECT id FROM blogs WHERE tenant_id = ?)",
            "DELETE FROM blog_revisions WHERE blog_id IN (SELECT id FROM blogs WHERE tenant_id = ?)",
            "DELETE FROM blog_comments WHERE tenant_id = ?",
            "DELETE FROM blogs WHERE tenant_id = ?",
            "DELETE FROM cms_pages WHERE tenant_id = ?",
            "DELETE FROM blog_authors WHERE tenant_id = ?",
            "DELETE FROM blog_tags WHERE tenant_id = ?",
            "DELETE FROM blog_categories WHERE tenant_id = ?",
            "DELETE FROM enquiries WHERE tenant_id = ?",
            "DELETE FROM media WHERE tenant_id = ?",
            "DELETE FROM folders WHERE tenant_id = ?",
            "DELETE FROM seo_metadata WHERE id NOT IN (SELECT seo_id FROM blogs) AND id NOT IN (SELECT seo_id FROM cms_pages)"
        };

        try (Connection conn = sourceDs.getConnection()) {
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            
            try {
                for (String sql : deleteQueries) {
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        int paramCount = sql.length() - sql.replace("?", "").length();
                        for (int i = 1; i <= paramCount; i++) {
                            stmt.setString(i, tenantId);
                        }
                        stmt.executeUpdate();
                    }
                }
                conn.commit();
                log.info("Successfully purged tenant {} data", tenantId);
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(autoCommit);
            }
        }
    }

    private String getSelectSqlForTable(String tableName) {
        switch (tableName) {
            case "analytics_metrics":
                return "SELECT * FROM analytics_metrics WHERE entity_id IN (SELECT id FROM blogs WHERE tenant_id = ?) OR entity_id IN (SELECT id FROM cms_pages WHERE tenant_id = ?)";
            case "analytics_view_logs":
                return "SELECT * FROM analytics_view_logs WHERE entity_id IN (SELECT id FROM blogs WHERE tenant_id = ?) OR entity_id IN (SELECT id FROM cms_pages WHERE tenant_id = ?)";
            case "blog_revisions":
                return "SELECT * FROM blog_revisions WHERE blog_id IN (SELECT id FROM blogs WHERE tenant_id = ?)";
            case "blog_sections":
                return "SELECT * FROM blog_sections WHERE blog_id IN (SELECT id FROM blogs WHERE tenant_id = ?)";
            case "blog_slug_redirects":
                return "SELECT * FROM blog_slug_redirects WHERE blog_id IN (SELECT id FROM blogs WHERE tenant_id = ?)";
            case "blog_sub_sections":
                return "SELECT * FROM blog_sub_sections WHERE section_id IN (SELECT id FROM blog_sections WHERE blog_id IN (SELECT id FROM blogs WHERE tenant_id = ?))";
            case "content_blocks":
                return "SELECT * FROM content_blocks WHERE page_id IN (SELECT id FROM cms_pages WHERE tenant_id = ?)";
            case "engagement_milestones":
                return "SELECT * FROM engagement_milestones WHERE entity_id IN (SELECT id FROM blogs WHERE tenant_id = ?) OR entity_id IN (SELECT id FROM cms_pages WHERE tenant_id = ?)";
            case "utm_campaign_metrics":
                return "SELECT * FROM utm_campaign_metrics WHERE entity_id IN (SELECT id FROM blogs WHERE tenant_id = ?) OR entity_id IN (SELECT id FROM cms_pages WHERE tenant_id = ?)";
            default:
                return "SELECT * FROM " + tableName + " WHERE tenant_id = ?";
        }
    }
}
