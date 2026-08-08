package com.quillforge.api.settings.repository;

import com.quillforge.api.settings.entity.CompanySetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CompanySettingRepository extends JpaRepository<CompanySetting, UUID> {
}
