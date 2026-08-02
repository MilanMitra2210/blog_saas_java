package com.quillforge.api.blog.controller;

import com.quillforge.api.blog.dto.*;
import com.quillforge.api.blog.service.BlogService;
import com.quillforge.api.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin/blogs")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Blogs Meta Management", description = "Endpoints for administrators to manage blog authors, tags, redirects, and revisions")
@RequiredArgsConstructor
public class AdminBlogMetaController {

    private final BlogService blogService;

    // Authors (Admin)
    @PostMapping("/authors")
    @Operation(summary = "Create blog author")
    public ResponseEntity<ApiResponse<BlogAuthorDto>> createAuthor(@RequestBody BlogAuthorDto dto) {
        BlogAuthorDto response = blogService.createAuthor(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Blog author created successfully", response));
    }

    @PutMapping("/authors/{id}")
    @Operation(summary = "Update blog author")
    public ResponseEntity<ApiResponse<BlogAuthorDto>> updateAuthor(@PathVariable UUID id, @RequestBody BlogAuthorDto dto) {
        BlogAuthorDto response = blogService.updateAuthor(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Blog author updated successfully", response));
    }

    @DeleteMapping("/authors/{id}")
    @Operation(summary = "Delete blog author")
    public ResponseEntity<ApiResponse<Void>> deleteAuthor(@PathVariable UUID id) {
        blogService.deleteAuthor(id);
        return ResponseEntity.ok(ApiResponse.success("Blog author soft-deleted successfully"));
    }

    // Tags (Admin)
    @PostMapping("/tags")
    @Operation(summary = "Create blog tag")
    public ResponseEntity<ApiResponse<TagDto>> createTag(@RequestBody TagDto dto) {
        TagDto response = blogService.createTag(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tag created successfully", response));
    }

    @GetMapping("/tags/{id}")
    @Operation(summary = "Get blog tag by ID")
    public ResponseEntity<ApiResponse<TagDto>> getTagById(@PathVariable UUID id) {
        TagDto response = tagRepositoryLookup(id);
        return ResponseEntity.ok(ApiResponse.success("Tag retrieved successfully", response));
    }

    private TagDto tagRepositoryLookup(UUID id) {
        return blogService.getAllTags().stream().filter(t -> t.getId().equals(id)).findFirst().orElse(null);
    }

    @PutMapping("/tags/{id}")
    @Operation(summary = "Update blog tag")
    public ResponseEntity<ApiResponse<TagDto>> updateTag(@PathVariable UUID id, @RequestBody TagDto dto) {
        TagDto response = blogService.updateTag(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Tag updated successfully", response));
    }

    @DeleteMapping("/tags/{id}")
    @Operation(summary = "Delete blog tag")
    public ResponseEntity<ApiResponse<Void>> deleteTag(@PathVariable UUID id) {
        blogService.deleteTag(id);
        return ResponseEntity.ok(ApiResponse.success("Tag deleted successfully"));
    }

    // Redirects (Admin)
    @PostMapping("/redirects")
    @Operation(summary = "Create blog slug redirect manually")
    public ResponseEntity<ApiResponse<Void>> createRedirect(@RequestBody Map<String, Object> payload) {
        String oldSlug = (String) payload.get("old_slug");
        String blogIdStr = (String) payload.get("blog_id");
        blogService.createManualRedirect(oldSlug, UUID.fromString(blogIdStr));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Blog redirect created successfully"));
    }

    @DeleteMapping("/redirects/{oldSlug}")
    @Operation(summary = "Delete blog slug redirect manually")
    public ResponseEntity<ApiResponse<Void>> deleteRedirect(@PathVariable String oldSlug) {
        blogService.deleteManualRedirect(oldSlug);
        return ResponseEntity.ok(ApiResponse.success("Blog redirect deleted successfully"));
    }

    // Revisions (Admin)
    @GetMapping("/{blogId}/revisions")
    @Operation(summary = "Get blog revisions history")
    public ResponseEntity<ApiResponse<List<BlogRevisionDto>>> getRevisions(@PathVariable UUID blogId) {
        List<BlogRevisionDto> revisions = blogService.getRevisions(blogId);
        return ResponseEntity.ok(ApiResponse.success("Revisions retrieved successfully", revisions));
    }

    @PostMapping("/{blogId}/revisions/{revisionId}/restore")
    @Operation(summary = "Restore blog post to revision")
    public ResponseEntity<ApiResponse<BlogResponse>> restoreRevision(@PathVariable UUID blogId, @PathVariable UUID revisionId) {
        BlogResponse response = blogService.restoreRevision(blogId, revisionId);
        return ResponseEntity.ok(ApiResponse.success("Blog post restored successfully", response));
    }
}
