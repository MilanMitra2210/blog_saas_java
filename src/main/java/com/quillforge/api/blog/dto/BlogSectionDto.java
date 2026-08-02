package com.quillforge.api.blog.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.quillforge.api.media.dto.MediaDto;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BlogSectionDto {
    private String title;
    private Object content;

    @JsonProperty("sectionType")
    private String sectionType = "content";

    @JsonProperty("displayOrder")
    private int displayOrder;

    private MediaDto media;

    @JsonProperty("ctaButtonText")
    private String ctaButtonText;

    @JsonProperty("ctaButtonUrl")
    private String ctaButtonUrl;

    @JsonProperty("embedUrl")
    private String embedUrl;

    @JsonProperty("embedProvider")
    private String embedProvider;

    @JsonProperty("codeBlock")
    private String codeBlock;

    @JsonProperty("codeLanguage")
    private String codeLanguage;

    @JsonProperty("subSections")
    private List<BlogSubSectionDto> subSections;
}
