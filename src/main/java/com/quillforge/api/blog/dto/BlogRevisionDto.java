package com.quillforge.api.blog.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class BlogRevisionDto {
    private UUID id;
    private UUID blogId;
    private String title;
    private String excerpt;

    @JsonProperty("sectionsData")
    private Object sectionsData;

    @JsonProperty("revisionNumber")
    private int revisionNumber;

    @JsonProperty("createdAt")
    private Instant createdAt;
}
