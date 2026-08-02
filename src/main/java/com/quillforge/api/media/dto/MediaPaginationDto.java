package com.quillforge.api.media.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MediaPaginationDto {
    private long total;
    private int page;
    private int limit;
    private int totalPages;
}
