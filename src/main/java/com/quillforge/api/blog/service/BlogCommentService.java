package com.quillforge.api.blog.service;

import com.quillforge.api.blog.dto.BlogCommentCreateDto;
import com.quillforge.api.blog.dto.BlogCommentResponseDto;
import com.quillforge.api.common.dto.PaginatedResponse;

import java.util.List;
import java.util.UUID;

public interface BlogCommentService {

    BlogCommentResponseDto createComment(BlogCommentCreateDto dto, String optionalAuthHeader);

    List<BlogCommentResponseDto> getApprovedCommentsForPost(UUID postId);

    PaginatedResponse<BlogCommentResponseDto> getAdminComments(
            int page,
            int limit,
            UUID postId,
            Boolean approved,
            String search
    );

    void approveComment(UUID commentId);

    void deleteComment(UUID commentId);
}
