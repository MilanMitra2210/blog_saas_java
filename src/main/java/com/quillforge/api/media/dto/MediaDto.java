package com.quillforge.api.media.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class MediaDto {
    private UUID id;
    private String name;

    @JsonProperty("altText")
    private String altText;

    private String key;
    private String url;
    private Long size;

    @JsonProperty("mimeType")
    private String mimeType;

    @JsonProperty("folderId")
    private UUID folderId;

    private Integer width;
    private Integer height;
    private Instant createdAt;
    private Instant updatedAt;
}
