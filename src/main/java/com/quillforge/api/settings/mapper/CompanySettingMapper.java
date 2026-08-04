package com.quillforge.api.settings.mapper;

import com.quillforge.api.settings.dto.CompanySettingResponseDto;
import com.quillforge.api.settings.dto.CompanySettingUpdateDto;
import com.quillforge.api.settings.entity.CompanySetting;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface CompanySettingMapper {

    CompanySettingResponseDto toDto(CompanySetting setting);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(CompanySettingUpdateDto dto, @MappingTarget CompanySetting setting);
}
