package com.quillforge.api.tenant;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import javax.sql.DataSource;

/**
 * Declares the primary routing datasource wrapping the default connection pool.
 */
@Configuration
public class TenantDataSourceConfig {

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.driver-class-name:org.postgresql.Driver}")
    private String driverClassName;

    @Bean(name = "defaultDataSource")
    public HikariDataSource defaultDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);
        config.setMaximumPoolSize(15);
        config.setMinimumIdle(5);
        config.setPoolName("SharedFreeDB-Pool");
        return new HikariDataSource(config);
    }

    @Bean
    @Primary
    public DataSource dataSource(HikariDataSource defaultDataSource) {
        return new TenantRoutingDataSource(defaultDataSource);
    }
}
