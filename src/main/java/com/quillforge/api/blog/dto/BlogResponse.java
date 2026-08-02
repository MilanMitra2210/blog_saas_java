package com.quillforge.api.blog.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.quillforge.api.common.dto.SeoDto;
import com.quillforge.api.media.dto.MediaDto;
import com.quillforge.api.user.dto.UserAuditDto;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class BlogResponse {
    private UUID id;
    private String title;
    private String slug;
    private String excerpt;

    @JsonProperty("publishDate")
    private Instant publishDate;

    @JsonProperty("readTime")
    private int readTime;

    @JsonProperty("isPublished")
    private boolean isPublished;

    @JsonProperty("isFeatured")
    private boolean isFeatured;

    @JsonProperty("displayOrder")
    private int displayOrder;

    @JsonProperty("avgRating")
    private double avgRating;

    @JsonProperty("totalRatings")
    private int totalRatings;

    private BlogCategoryDto category;
    private BlogAuthorDto author;

    @JsonProperty("bannerImage")
    private MediaDto bannerImage;

    private SeoDto seo;

    @JsonProperty("sections")
    private List<BlogSectionDto> sections;

    @JsonProperty("tags")
    private List<TagDto> tags;

    @JsonProperty("metrics")
    private BlogMetricDto metrics;

    @JsonProperty("createdAt")
    private Instant createdAt;

    @JsonProperty("updatedAt")
    private Instant updatedAt;

    @JsonProperty("createdBy")
    private UserAuditDto createdBy;

    @JsonProperty("updatedBy")
    private UserAuditDto updatedBy;
}
