package com.quillforge.api.common.mapper;

import com.quillforge.api.common.dto.SeoDto;
import com.quillforge.api.common.entity.SeoMetadata;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SeoMapper {
    SeoDto toDto(SeoMetadata seo);
    SeoMetadata toEntity(SeoDto dto);
}
