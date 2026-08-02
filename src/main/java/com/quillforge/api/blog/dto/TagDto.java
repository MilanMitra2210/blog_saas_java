package com.quillforge.api.blog.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class TagDto {
    private UUID id;
    private String name;
    private String slug;
    private Instant createdAt;
    private Instant updatedAt;
}
