package com.quillforge.api.media.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class MediaResponseDto {
    private List<MediaDto> files;
    private List<FolderDto> folders;
    private MediaPaginationDto pagination;
}
