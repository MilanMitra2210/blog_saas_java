package com.quillforge.api.cms.controller;

import com.quillforge.api.cms.dto.CMSPageResponse;
import com.quillforge.api.cms.service.CMSPageService;
import com.quillforge.api.common.dto.ApiResponse;
import com.quillforge.api.common.dto.PaginatedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pages")
@Tag(name = "Storefront CMS Pages", description = "Public endpoints for CMS pages")
@RequiredArgsConstructor
public class PublicCMSPageController {

    private final CMSPageService cmsPageService;

    @GetMapping
    @Operation(summary = "List CMS Pages", description = "Returns a paginated list of CMS pages, optionally filtered by status and search terms")
    public ResponseEntity<ApiResponse<PaginatedResponse<CMSPageResponse>>> getPages(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int limit) {

        Pageable pageable = PageRequest.of(page > 0 ? page - 1 : 0, limit, Sort.by("createdAt").descending());
        Page<CMSPageResponse> result = cmsPageService.getCMSPages(search, status, pageable);

        return ResponseEntity.ok(
                ApiResponse.success("CMS Pages retrieved successfully", PaginatedResponse.from(result))
        );
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get CMS Page by slug", description = "Retrieves a single CMS Page's details by its URL slug")
    public ResponseEntity<ApiResponse<CMSPageResponse>> getPageBySlug(@PathVariable String slug) {
        CMSPageResponse response = cmsPageService.getCMSPageBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success("CMS Page retrieved successfully", response));
    }
}
