package com.quillforge.api.settings.service;

import com.quillforge.api.settings.dto.CompanySettingResponseDto;
import com.quillforge.api.settings.dto.CompanySettingUpdateDto;

public interface CompanySettingService {
    CompanySettingResponseDto getCompanySettings();
    CompanySettingResponseDto updateCompanySettings(CompanySettingUpdateDto dto);
}
