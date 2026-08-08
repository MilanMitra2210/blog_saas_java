package com.quillforge.api.settings.service;

import com.quillforge.api.settings.dto.CompanySettingResponseDto;
import com.quillforge.api.settings.dto.CompanySettingUpdateDto;
import com.quillforge.api.settings.entity.CompanySetting;
import com.quillforge.api.settings.mapper.CompanySettingMapper;
import com.quillforge.api.settings.repository.CompanySettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.quillforge.api.common.service.RevalidationService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanySettingServiceImpl implements CompanySettingService {

    private final CompanySettingRepository companySettingRepository;
    private final CompanySettingMapper companySettingMapper;
    private final RevalidationService revalidationService;

    @Override
    @Transactional
    @Cacheable(value = "company_settings", key = "T(com.quillforge.api.common.config.TenantContext).getCurrentTenant()")
    public CompanySettingResponseDto getCompanySettings() {
        CompanySetting setting = getOrCreateInstance();
        return companySettingMapper.toDto(setting);
    }

    @Override
    @Transactional
    @CacheEvict(value = "company_settings", key = "T(com.quillforge.api.common.config.TenantContext).getCurrentTenant()")
    public CompanySettingResponseDto updateCompanySettings(CompanySettingUpdateDto dto) {
        CompanySetting setting = getOrCreateInstance();
        companySettingMapper.updateEntityFromDto(dto, setting);
        CompanySetting saved = companySettingRepository.save(setting);
        revalidationService.revalidate("company_settings", null, "update");
        return companySettingMapper.toDto(saved);
    }

    private CompanySetting getOrCreateInstance() {
        return companySettingRepository.findAll().stream()
                .findFirst()
                .orElseGet(() -> {
                    CompanySetting s = new CompanySetting();
                    s.setCompanyName("My Company");
                    s.setEmail("");
                    s.setWhatsappNumber("");
                    s.setWhatsappMessage("");
                    s.setRegisteredOfficeAddress("");
                    s.setManufacturingPlantAddress("");
                    s.setCin("");
                    s.setMapUrl("");
                    s.setFacebookUrl("");
                    s.setInstagramUrl("");
                    s.setPinterestUrl("");
                    s.setLinkedinUrl("");
                    s.setTwitterUrl("");
                    s.setYoutubeUrl("");
                    s.setGithubUrl("");
                    s.setTiktokUrl("");
                    return companySettingRepository.save(s);
                });
    }
}
