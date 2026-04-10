package com.workhub.controller;

import com.workhub.security.TenantContext;
import com.workhub.util.TenantContextHolder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tenant Info Controller
 * 
 * Demonstrates how to access tenant context using TenantContext.getTenantId()
 */
@RestController
@RequestMapping("/api/tenant")
@RequiredArgsConstructor
@Slf4j
public class TenantInfoController {

    /**
     * Get current tenant information
     * 
     * GET /api/tenant/info
     * 
     * Demonstrates multiple ways to access tenant ID:
     * 1. TenantContext.getTenantId() - Direct access (returns null if not set)
     * 2. TenantContext.getRequiredTenantId() - Throws exception if not set
     * 3. TenantContextHolder.getCurrentTenantId() - Utility method
     * 
     * @return Tenant information
     */
    @GetMapping("/info")
    public ResponseEntity<TenantInfo> getTenantInfo() {
        log.info("Getting tenant info");

        // Method 1: Direct access using TenantContext.getTenantId()
        Long tenantIdDirect = TenantContext.getTenantId();
        
        // Method 2: Using TenantContextHolder (recommended)
        Long tenantIdHolder = TenantContextHolder.getCurrentTenantId();
        
        // Method 3: Check if tenant context exists
        boolean hasTenant = TenantContext.hasTenantId();
        
        // Method 4: Get required (throws exception if null)
        Long tenantIdRequired = null;
        try {
            tenantIdRequired = TenantContext.getRequiredTenantId();
        } catch (IllegalStateException e) {
            log.warn("Tenant context not set: {}", e.getMessage());
        }

        TenantInfo info = new TenantInfo(
                tenantIdDirect,
                tenantIdHolder,
                tenantIdRequired,
                hasTenant,
                TenantContextHolder.getTenantIdAsString()
        );

        log.info("Tenant info: {}", info);
        return ResponseEntity.ok(info);
    }

    /**
     * Simple endpoint to get just the tenant ID
     * 
     * GET /api/tenant/id
     * 
     * @return Current tenant ID
     */
    @GetMapping("/id")
    public ResponseEntity<TenantIdResponse> getTenantId() {
        // Direct usage of TenantContext.getTenantId()
        Long tenantId = TenantContext.getTenantId();
        
        if (tenantId == null) {
            return ResponseEntity.ok(new TenantIdResponse(null, "No tenant context"));
        }
        
        return ResponseEntity.ok(new TenantIdResponse(tenantId, "OK"));
    }

    /**
     * Check if tenant context is available
     * 
     * GET /api/tenant/check
     * 
     * @return Tenant context status
     */
    @GetMapping("/check")
    public ResponseEntity<TenantCheckResponse> checkTenantContext() {
        boolean hasTenant = TenantContext.hasTenantId();
        Long tenantId = TenantContext.getTenantId();
        
        return ResponseEntity.ok(new TenantCheckResponse(
                hasTenant,
                tenantId,
                hasTenant ? "Tenant context is set" : "No tenant context"
        ));
    }

    // Response DTOs
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TenantInfo {
        private Long tenantIdDirect;
        private Long tenantIdFromHolder;
        private Long tenantIdRequired;
        private boolean hasTenantContext;
        private String tenantIdAsString;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TenantIdResponse {
        private Long tenantId;
        private String status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TenantCheckResponse {
        private boolean hasTenantContext;
        private Long tenantId;
        private String message;
    }
}
