package com.workhub.controller;

import com.workhub.dto.CreateTenantRequest;
import com.workhub.dto.TenantResponse;
import com.workhub.security.TenantContext;
import com.workhub.service.TenantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/tenants")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @PostMapping
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<TenantResponse> createTenant(@Valid @RequestBody CreateTenantRequest request) {
        TenantResponse response = tenantService.createTenant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<TenantResponse> currentTenant() {
        return ResponseEntity.ok(tenantService.getCurrentTenant());
    }

    @GetMapping("/context")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<Map<String, String>> tenantContext() {
        Long tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(Map.of("tenantId", tenantId == null ? "not-set" : tenantId.toString()));
    }
}
