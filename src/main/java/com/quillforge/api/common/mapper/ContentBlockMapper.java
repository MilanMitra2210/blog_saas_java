package com.quillforge.api.common.mapper;

import com.quillforge.api.common.dto.ContentBlockDto;
import com.quillforge.api.common.entity.ContentBlock;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ContentBlockMapper {
    @Mapping(source = "blockKey", target = "blockKey")
    ContentBlockDto toDto(ContentBlock block);

    @Mapping(source = "blockKey", target = "blockKey")
    ContentBlock toEntity(ContentBlockDto dto);
}
