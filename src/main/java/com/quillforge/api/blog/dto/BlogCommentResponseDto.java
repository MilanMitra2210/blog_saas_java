package com.quillforge.api.blog.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class BlogCommentResponseDto {
    private UUID id;

    @JsonProperty("postId")
    private UUID postId;

    @JsonProperty("postTitle")
    private String postTitle;

    @JsonProperty("postSlug")
    private String postSlug;

    @JsonProperty("authorName")
    private String authorName;

    @JsonProperty("authorEmail")
    private String authorEmail;

    private String content;
    private boolean approved;

    @JsonProperty("parentId")
    private UUID parentId;

    @JsonProperty("isAdmin")
    private boolean isAdmin;

    @JsonProperty("createdAt")
    private Instant createdAt;

    private List<BlogCommentResponseDto> replies;
}
