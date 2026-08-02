package com.quillforge.api.blog.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.quillforge.api.common.dto.SeoDto;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class BlogRequest {

    @NotBlank(message = "Blog title is required")
    private String title;

    @NotBlank(message = "Blog slug is required")
    private String slug;

    private String excerpt;

    @JsonProperty("publishDate")
    private Instant publishDate;

    @JsonProperty("readTime")
    private int readTime = 5;

    @JsonProperty("isPublished")
    private boolean isPublished = false;

    @JsonProperty("isFeatured")
    private boolean isFeatured = false;

    @JsonProperty("displayOrder")
    private int displayOrder = 0;

    @JsonProperty("categoryId")
    private UUID categoryId;

    @JsonProperty("authorId")
    private UUID authorId;

    @JsonProperty("bannerImageId")
    private UUID bannerImageId;

    private SeoDto seo;

    @JsonProperty("sections")
    private List<BlogSectionDto> sections;

    @JsonProperty("tagIds")
    private List<UUID> tagIds;
}
