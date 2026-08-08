package com.quillforge.api.settings.repository;

import com.quillforge.api.settings.entity.CompanySetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CompanySettingRepository extends JpaRepository<CompanySetting, UUID> {

    @Query(value = "SELECT tenant_id FROM company_settings WHERE subdomain = :subdomain AND deleted = false LIMIT 1", nativeQuery = true)
    Optional<String> findTenantIdBySubdomain(@Param("subdomain") String subdomain);

    @Query(value = "SELECT tenant_id FROM company_settings WHERE custom_domain = :customDomain AND deleted = false LIMIT 1", nativeQuery = true)
    Optional<String> findTenantIdByCustomDomain(@Param("customDomain") String customDomain);
}
