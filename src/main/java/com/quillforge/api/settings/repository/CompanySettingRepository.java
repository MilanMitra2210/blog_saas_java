package com.quillforge.api.settings.repository;

import com.quillforge.api.settings.entity.CompanySetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CompanySettingRepository extends JpaRepository<CompanySetting, UUID> {
}
