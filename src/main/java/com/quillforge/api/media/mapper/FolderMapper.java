package com.quillforge.api.media.mapper;

import com.quillforge.api.media.dto.FolderDto;
import com.quillforge.api.media.entity.Folder;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FolderMapper {
    FolderDto toDto(Folder folder);
    Folder toEntity(FolderDto dto);
}
