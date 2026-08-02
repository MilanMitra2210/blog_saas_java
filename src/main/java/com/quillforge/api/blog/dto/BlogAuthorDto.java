package com.quillforge.api.blog.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.quillforge.api.media.dto.MediaDto;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class BlogAuthorDto {
    private UUID id;
    private String name;
    private String bio;
    private String designation;
    private UUID imageId;
    private MediaDto image;
    private Instant createdAt;
    private Instant updatedAt;
}
