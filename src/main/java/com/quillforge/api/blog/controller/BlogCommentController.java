package com.quillforge.api.blog.controller;

import com.quillforge.api.blog.dto.BlogCommentCreateDto;
import com.quillforge.api.blog.dto.BlogCommentResponseDto;
import com.quillforge.api.blog.service.BlogCommentService;
import com.quillforge.api.common.dto.ApiResponse;
import com.quillforge.api.common.dto.PaginatedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Blog Comments", description = "Endpoints for posting, retrieving and moderating blog post comments")
public class BlogCommentController {

    private final BlogCommentService blogCommentService;

    // --- Public Storefront Endpoints ---

    @PostMapping("/comments")
    @Operation(summary = "Add a comment or reply to a post")
    public ResponseEntity<ApiResponse<BlogCommentResponseDto>> createComment(
            @Valid @RequestBody BlogCommentCreateDto dto,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        BlogCommentResponseDto response = blogCommentService.createComment(dto, authHeader);
        String detail = response.isApproved() ? "Comment posted successfully" : "Comment submitted for approval";
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(detail, response));
    }

    @GetMapping("/comments")
    @Operation(summary = "List approved comments for a blog post")
    public ResponseEntity<ApiResponse<List<BlogCommentResponseDto>>> getComments(
            @RequestParam("postId") UUID postId
    ) {
        List<BlogCommentResponseDto> comments = blogCommentService.getApprovedCommentsForPost(postId);
        return ResponseEntity.ok(ApiResponse.success("Comments retrieved", comments));
    }

    // --- Admin Moderation Endpoints ---

    @GetMapping("/admin/comments")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "List all comments with moderation details (Admin/Editor)")
    public ResponseEntity<ApiResponse<PaginatedResponse<BlogCommentResponseDto>>> getAdminComments(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            @RequestParam(value = "postId", required = false) UUID postId,
            @RequestParam(value = "approved", required = false) Boolean approved,
            @RequestParam(value = "search", required = false) String search
    ) {
        PaginatedResponse<BlogCommentResponseDto> response = blogCommentService.getAdminComments(page, limit, postId, approved, search);
        return ResponseEntity.ok(ApiResponse.success("Comments retrieved for admin", response));
    }

    @PutMapping("/admin/comments/{commentId}/approve")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Approve a comment (Admin/Editor)")
    public ResponseEntity<ApiResponse<Void>> approveComment(
            @PathVariable("commentId") UUID commentId
    ) {
        blogCommentService.approveComment(commentId);
        return ResponseEntity.ok(ApiResponse.success("Comment approved successfully"));
    }

    @DeleteMapping("/admin/comments/{commentId}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete a comment (Admin/Editor)")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable("commentId") UUID commentId
    ) {
        blogCommentService.deleteComment(commentId);
        return ResponseEntity.ok(ApiResponse.success("Comment deleted successfully"));
    }
}
