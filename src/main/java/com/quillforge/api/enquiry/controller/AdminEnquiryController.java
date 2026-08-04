package com.quillforge.api.enquiry.controller;

import com.quillforge.api.common.dto.ApiResponse;
import com.quillforge.api.common.dto.PaginatedResponse;
import com.quillforge.api.enquiry.dto.EnquiryResponseDto;
import com.quillforge.api.enquiry.dto.EnquiryUpdateDto;
import com.quillforge.api.enquiry.service.EnquiryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin/enquiries")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Enquiries", description = "Administrative endpoints for managing contact/support enquiries")
@RequiredArgsConstructor
public class AdminEnquiryController {

    private final EnquiryService enquiryService;

    @GetMapping
    @PreAuthorize("hasAuthority('enquiries:read')")
    @Operation(summary = "List all enquiries with paginated search and filters")
    public ResponseEntity<ApiResponse<PaginatedResponse<EnquiryResponseDto>>> getAllEnquiries(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "type", required = false) String type
    ) {
        PaginatedResponse<EnquiryResponseDto> response = enquiryService.getAllEnquiries(page, limit, search, status, type);
        return ResponseEntity.ok(ApiResponse.success("Enquiries retrieved", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('enquiries:read')")
    @Operation(summary = "Get detailed view of a single enquiry by ID")
    public ResponseEntity<ApiResponse<EnquiryResponseDto>> getEnquiryById(@PathVariable("id") UUID id) {
        EnquiryResponseDto response = enquiryService.getEnquiryById(id);
        return ResponseEntity.ok(ApiResponse.success("Enquiry retrieved", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('enquiries:write')")
    @Operation(summary = "Update status and notes of an enquiry")
    public ResponseEntity<ApiResponse<EnquiryResponseDto>> updateEnquiry(
            @PathVariable("id") UUID id,
            @RequestBody EnquiryUpdateDto dto
    ) {
        EnquiryResponseDto response = enquiryService.updateEnquiry(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Enquiry updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('enquiries:write')")
    @Operation(summary = "Delete an enquiry by ID")
    public ResponseEntity<ApiResponse<Void>> deleteEnquiry(@PathVariable("id") UUID id) {
        enquiryService.deleteEnquiry(id);
        return ResponseEntity.ok(ApiResponse.success("Enquiry deleted successfully"));
    }
}
