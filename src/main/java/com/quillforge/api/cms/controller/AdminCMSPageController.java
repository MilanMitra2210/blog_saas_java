package com.quillforge.api.cms.controller;

import com.quillforge.api.cms.dto.CMSPageRequest;
import com.quillforge.api.cms.dto.CMSPageResponse;
import com.quillforge.api.cms.service.CMSPageService;
import com.quillforge.api.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/admin/pages")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin CMS Page Management", description = "Endpoints for administrators to manage CMS pages")
@RequiredArgsConstructor
public class AdminCMSPageController {

    private final CMSPageService cmsPageService;

    @PostMapping
    @Operation(summary = "Create CMS Page", description = "Creates a CMS page with seo metadata and content blocks")
    public ResponseEntity<ApiResponse<CMSPageResponse>> createPage(
            @RequestBody @Valid CMSPageRequest request) {
        CMSPageResponse response = cmsPageService.createCMSPage(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("CMS Page created successfully", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update CMS Page", description = "Updates an existing CMS page record")
    public ResponseEntity<ApiResponse<CMSPageResponse>> updatePage(
            @PathVariable UUID id,
            @RequestBody @Valid CMSPageRequest request) {
        CMSPageResponse response = cmsPageService.updateCMSPage(id, request);
        return ResponseEntity.ok(ApiResponse.success("CMS Page updated successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get CMS Page by ID", description = "Fetches details of a CMS page record by its UUID")
    public ResponseEntity<ApiResponse<CMSPageResponse>> getPageById(@PathVariable UUID id) {
        CMSPageResponse response = cmsPageService.getCMSPageById(id);
        return ResponseEntity.ok(ApiResponse.success("CMS Page retrieved successfully", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete CMS Page", description = "Performs soft delete on a CMS page")
    public ResponseEntity<ApiResponse<Void>> deletePage(@PathVariable UUID id) {
        cmsPageService.deleteCMSPage(id);
        return ResponseEntity.ok(ApiResponse.success("CMS Page deleted successfully"));
    }
}
