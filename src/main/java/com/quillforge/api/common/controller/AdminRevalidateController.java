package com.quillforge.api.common.controller;

import com.quillforge.api.common.dto.ApiResponse;
import com.quillforge.api.common.service.RevalidationService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/revalidate")
@RequiredArgsConstructor
public class AdminRevalidateController {

    private final RevalidationService revalidationService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> triggerManualRevalidate(
            @RequestBody RevalidateRequest request
    ) {
        revalidationService.revalidate(
                request.getModel(),
                request.getSlug(),
                request.getAction() != null ? request.getAction() : "manual"
        );
        return ResponseEntity.ok(ApiResponse.success("Revalidation triggered successfully"));
    }

    @GetMapping("/models")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRevalidateModels() {
        List<Map<String, Object>> models = List.of(
            Map.of("name", "blog", "label", "Blog Post", "slugPrefix", "/blogs/"),
            Map.of("name", "cms_page", "label", "CMS Static Page", "slugPrefix", "/"),
            Map.of("name", "blog_category", "label", "Blog Category", "slugPrefix", ""),
            Map.of("name", "company_settings", "label", "Company Settings", "slugPrefix", "")
        );
        return ResponseEntity.ok(ApiResponse.success("Models retrieved", models));
    }

    @Getter
    @Setter
    public static class RevalidateRequest {
        private String model;
        private String slug;
        private String action;
    }
}
