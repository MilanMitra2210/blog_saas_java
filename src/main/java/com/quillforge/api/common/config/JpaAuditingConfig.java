package com.quillforge.api.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import org.springframework.beans.factory.ObjectProvider;

/**
 * Enables JPA Auditing. The auditorAware bean is provided by AuditorAwareImpl @Component.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

    @Bean
    public HibernatePropertiesCustomizer jsonFormatMapperCustomizer(ObjectProvider<ObjectMapper> objectMapperProvider) {
        return (properties) -> {
            properties.put("hibernate.type.json_format_mapper", new JacksonFormatMapper(objectMapperProvider.getIfAvailable(ObjectMapper::new)));
        };
    }
}
