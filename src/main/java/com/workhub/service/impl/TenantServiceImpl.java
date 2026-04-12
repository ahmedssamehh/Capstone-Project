package com.workhub.service.impl;

import com.workhub.dto.CreateTenantRequest;
import com.workhub.dto.TenantResponse;
import com.workhub.entity.Tenant;
import com.workhub.repository.TenantRepository;
import com.workhub.service.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TenantServiceImpl implements TenantService {

    private final TenantRepository tenantRepository;

    @Override
    public TenantResponse createTenant(CreateTenantRequest request) {
        String normalizedName = request.getName().trim();
        if (tenantRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new IllegalArgumentException("Tenant name already exists: " + normalizedName);
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
    public List<TenantResponse> listTenants() {
        return tenantRepository.findAll().stream()
                .map(tenant -> TenantResponse.builder()
                        .id(tenant.getId())
                        .name(tenant.getName())
                        .slug(slugify(tenant.getName()))
                        .build())
                .toList();
    }

    private String slugify(String input) {
        String normalized = input.toLowerCase(Locale.ROOT).trim();
        return normalized.replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}