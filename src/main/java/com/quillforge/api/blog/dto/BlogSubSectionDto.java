package com.quillforge.api.blog.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.quillforge.api.media.dto.MediaDto;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BlogSubSectionDto {
    private String title;
    private Object content;

    @JsonProperty("displayOrder")
    private int displayOrder;

    private MediaDto media;
}
