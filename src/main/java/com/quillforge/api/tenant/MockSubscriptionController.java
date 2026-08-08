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
