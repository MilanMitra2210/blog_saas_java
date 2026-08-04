package com.quillforge.api.settings.controller;

import com.quillforge.api.common.dto.ApiResponse;
import com.quillforge.api.settings.dto.CompanySettingResponseDto;
import com.quillforge.api.settings.dto.CompanySettingUpdateDto;
import com.quillforge.api.settings.service.CompanySettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/company-settings")
@Tag(name = "Company Settings", description = "Endpoints for managing global company settings configuration")
@RequiredArgsConstructor
public class CompanySettingController {

    private final CompanySettingService companySettingService;

    @GetMapping
    @Operation(summary = "Retrieve global company settings singleton configuration")
    public ResponseEntity<ApiResponse<CompanySettingResponseDto>> getCompanySettings() {
        CompanySettingResponseDto response = companySettingService.getCompanySettings();
        return ResponseEntity.ok(ApiResponse.success("Company settings retrieved", response));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('company_settings:write')")
    @Operation(summary = "Update global company settings configuration")
    public ResponseEntity<ApiResponse<CompanySettingResponseDto>> updateCompanySettings(
            @RequestBody CompanySettingUpdateDto dto
    ) {
        CompanySettingResponseDto response = companySettingService.updateCompanySettings(dto);
        return ResponseEntity.ok(ApiResponse.success("Company settings updated successfully", response));
    }
}
