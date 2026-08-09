package com.quillforge.api.tenant;

import com.quillforge.api.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller to simulate Stripe billing events and trigger backend data migrations.
 */
@RestController
@RequestMapping("/admin/mock-subscription")
@RequiredArgsConstructor
@Tag(name = "Billing & Subscription Simulator", description = "Endpoints to simulate subscription events and database migrations")
public class MockSubscriptionController {

    private final TenantMigrationService migrationService;
    private final com.quillforge.api.settings.repository.CompanySettingRepository companySettingRepository;

    @GetMapping("/tenants")
    @Operation(summary = "Get registered client tenants", description = "Retrieves all active client tenant IDs registered in the system.")
    public ResponseEntity<ApiResponse<java.util.List<String>>> getClientTenants() {
        String originalTenant = TenantContext.getCurrentTenant();
        TenantContext.setCurrentTenant(TenantContext.DEFAULT_TENANT);
        try {
            java.util.List<String> tenants = companySettingRepository.findActiveClientTenantIds();
            return ResponseEntity.ok(ApiResponse.success("Client tenants retrieved successfully", tenants));
        } finally {
            TenantContext.setCurrentTenant(originalTenant);
        }
    }

    @PostMapping("/upgrade")
    @Operation(summary = "Simulate PRO tier upgrade", description = "Creates a dedicated PostgreSQL database, bootstraps tables, and migrates row-level data.")
    public ResponseEntity<ApiResponse<String>> mockUpgrade(@RequestParam("tenantId") String tenantId) {
        if (tenantId == null || tenantId.trim().isEmpty() || "default".equals(tenantId)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid tenant ID provided"));
        }
        
        migrationService.migrateTenantToDedicatedDatabase(tenantId);
        return ResponseEntity.ok(ApiResponse.success("Tenant database provisioned and data migrated successfully", tenantId));
    }

    @PostMapping("/cancel")
    @Operation(summary = "Simulate subscription cancellation", description = "Simulates downgrading a tenant's billing tier (limits apply, data remains in dedicated DB).")
    public ResponseEntity<ApiResponse<String>> mockCancel(@RequestParam("tenantId") String tenantId) {
        if (tenantId == null || tenantId.trim().isEmpty() || "default".equals(tenantId)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid tenant ID provided"));
        }
        // In the cancel event, data stays on their isolated DB, but subscription tier is downgraded
        return ResponseEntity.ok(ApiResponse.success("Subscription cancelled. Tenant remains on isolated database with downgraded limits.", tenantId));
    }
}
