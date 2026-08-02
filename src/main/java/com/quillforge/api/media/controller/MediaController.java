package com.quillforge.api.media.controller;

import com.quillforge.api.common.dto.ApiResponse;
import com.quillforge.api.media.dto.FolderDto;
import com.quillforge.api.media.dto.MediaDto;
import com.quillforge.api.media.dto.MediaResponseDto;
import com.quillforge.api.media.service.FolderService;
import com.quillforge.api.media.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/uploads")
@RequiredArgsConstructor
@Tag(name = "Media Manager", description = "Endpoints for uploading, organizing, and managing files and folders")
public class MediaController {

    private final MediaService mediaService;
    private final FolderService folderService;

    @GetMapping
    @Operation(summary = "Get media library content", description = "Retrieves paginated files and folders for a specific folder level")
    public ResponseEntity<ApiResponse<MediaResponseDto>> getMedia(
            @RequestParam(value = "folder_id", required = false) UUID folderId,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "10") int limit
    ) {
        MediaResponseDto response = mediaService.getMedia(folderId, search, page, limit);
        return ResponseEntity.ok(ApiResponse.success("Media retrieved", response));
    }

    @PostMapping({"", "/"})
    @Operation(summary = "Upload single media item", description = "Uploads a single multipart file to S3")
    public ResponseEntity<ApiResponse<MediaDto>> uploadMedia(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder_id", required = false) UUID folderId,
            @RequestParam(value = "alt_text", required = false) String altText
    ) {
        MediaDto media = mediaService.uploadMedia(file, folderId, altText);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("File uploaded successfully", media));
    }

    @PostMapping("/bulk")
    @Operation(summary = "Bulk upload media items", description = "Uploads multiple multipart files simultaneously")
    public ResponseEntity<ApiResponse<List<MediaDto>>> uploadMediaBulk(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "folder_id", required = false) UUID folderId
    ) {
        List<MediaDto> mediaList = mediaService.uploadMediaBulk(files, folderId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Files uploaded successfully", mediaList));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace media file", description = "Overwrites S3 object and metadata for an existing item")
    public ResponseEntity<ApiResponse<MediaDto>> replaceMedia(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file
    ) {
        MediaDto media = mediaService.replaceMedia(id, file);
        return ResponseEntity.ok(ApiResponse.success("File replaced successfully", media));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update media metadata", description = "Patches title, alt text, or moves file to a different folder")
    public ResponseEntity<ApiResponse<MediaDto>> updateMedia(
            @PathVariable UUID id,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "alt_text", required = false) String altText,
            @RequestParam(value = "folder_id", required = false) UUID folderId
    ) {
        MediaDto media = mediaService.updateMedia(id, name, altText, folderId);
        return ResponseEntity.ok(ApiResponse.success("Metadata updated successfully", media));
    }

    @PatchMapping("/bulk-move")
    @Operation(summary = "Bulk move media items", description = "Moves multiple files to a target folder")
    public ResponseEntity<ApiResponse<Void>> bulkMoveMedia(@RequestBody Map<String, Object> payload) {
        // Safe mapping
        List<String> rawIds = (List<String>) payload.get("media_ids");
        String rawFolderId = (String) payload.get("folder_id");

        List<UUID> mediaIds = rawIds.stream().map(UUID::fromString).toList();
        UUID folderId = (rawFolderId != null && !rawFolderId.isEmpty()) ? UUID.fromString(rawFolderId) : null;

        mediaService.bulkMoveMedia(mediaIds, folderId);
        return ResponseEntity.ok(ApiResponse.success("Files moved successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete media item", description = "Permanently removes file from S3 and marks metadata as deleted")
    public ResponseEntity<ApiResponse<Void>> deleteMedia(@PathVariable UUID id) {
        mediaService.deleteMedia(id);
        return ResponseEntity.ok(ApiResponse.success("File deleted successfully"));
    }

    @GetMapping("/folders/all")
    @Operation(summary = "Get all folders", description = "Retrieves structural hierarchy folders list")
    public ResponseEntity<ApiResponse<List<FolderDto>>> getAllFolders() {
        List<FolderDto> folders = folderService.getAllFolders();
        return ResponseEntity.ok(ApiResponse.success("Folders retrieved", folders));
    }

    @PostMapping("/folders")
    @Operation(summary = "Create folder", description = "Creates a new catalog directory")
    public ResponseEntity<ApiResponse<FolderDto>> createFolder(
            @RequestParam("name") String name,
            @RequestParam(value = "parent_id", required = false) UUID parentId
    ) {
        FolderDto folder = folderService.createFolder(name, parentId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Folder created successfully", folder));
    }

    @DeleteMapping("/folders/{id}")
    @Operation(summary = "Delete folder", description = "Deletes folder and recursively clears all children")
    public ResponseEntity<ApiResponse<Void>> deleteFolder(@PathVariable UUID id) {
        folderService.deleteFolder(id);
        return ResponseEntity.ok(ApiResponse.success("Folder deleted successfully"));
    }
}
