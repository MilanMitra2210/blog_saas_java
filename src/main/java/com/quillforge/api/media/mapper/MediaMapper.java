package com.quillforge.api.media.mapper;

import com.quillforge.api.media.dto.MediaDto;
import com.quillforge.api.media.entity.Media;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MediaMapper {
    MediaDto toDto(Media media);
    Media toEntity(MediaDto dto);
}
