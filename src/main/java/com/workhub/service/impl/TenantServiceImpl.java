package com.workhub.service.impl;

import com.workhub.dto.CreateTenantRequest;
import com.workhub.dto.TenantResponse;
import com.workhub.entity.Tenant;
import com.workhub.exception.DuplicateResourceException;
import com.workhub.exception.ResourceNotFoundException;
import com.workhub.repository.TenantRepository;
import com.workhub.security.TenantContext;
import com.workhub.service.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(rollbackFor = Exception.class)
public class TenantServiceImpl implements TenantService {

    private final TenantRepository tenantRepository;

    @Override
    public TenantResponse createTenant(CreateTenantRequest request) {
        String normalizedName = request.getName().trim();
        if (tenantRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new DuplicateResourceException("Tenant name already exists: " + normalizedName);
        }

        Tenant tenant = Tenant.builder()
                .name(normalizedName)
                .plan(Tenant.TenantPlan.FREE)
                .status(Tenant.TenantStatus.ACTIVE)
                .build();

        Tenant saved = tenantRepository.save(tenant);
        log.info("Created tenant {} ({})", saved.getId(), saved.getName());

        return TenantResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .slug(slugify(saved.getName()))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public TenantResponse getCurrentTenant() {
        Long tenantId = TenantContext.getRequiredTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
        return TenantResponse.builder()
                .id(tenant.getId())
                .name(tenant.getName())
                .slug(slugify(tenant.getName()))
                .build();
    }

    private String slugify(String input) {
        String normalized = input.toLowerCase(Locale.ROOT).trim();
        return normalized.replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}