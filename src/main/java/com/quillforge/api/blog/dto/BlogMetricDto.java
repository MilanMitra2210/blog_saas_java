package com.quillforge.api.blog.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class BlogMetricDto {
    private UUID id;
    private UUID blogId;
    private int views;
    private int likes;

    @JsonProperty("readProgressCount")
    private int readProgressCount;
}
