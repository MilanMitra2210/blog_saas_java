package com.quillforge.api.blog.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class BlogCommentCreateDto {

    @NotNull(message = "Post ID is required")
    @JsonProperty("postId")
    private UUID postId;

    @NotBlank(message = "Author name is required")
    @JsonProperty("authorName")
    private String authorName;

    @NotBlank(message = "Author email is required")
    @Email(message = "Invalid email format")
    @JsonProperty("authorEmail")
    private String authorEmail;

    @NotBlank(message = "Comment content is required")
    @JsonProperty("content")
    private String content;

    @JsonProperty("parentId")
    private UUID parentId;
}
