package com.workhub.service;

import com.workhub.dto.CreateTenantRequest;
import com.workhub.dto.TenantResponse;
import com.workhub.entity.Tenant;
import com.workhub.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TenantServiceImpl implements TenantService {

    private final TenantRepository tenantRepository;

    public TenantServiceImpl(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Override
    @Transactional
    public TenantResponse createTenant(CreateTenantRequest request) {
        if (tenantRepository.existsBySlug(request.getSlug())) {
            throw new IllegalArgumentException("A tenant with this slug already exists");
        }

        Tenant tenant = Tenant.builder()
            .name(request.getName().trim())
            .slug(request.getSlug().trim().toLowerCase())
            .build();

        Tenant savedTenant = tenantRepository.save(tenant);
        return toResponse(savedTenant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TenantResponse> listTenants() {
        return tenantRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    private TenantResponse toResponse(Tenant tenant) {
        return TenantResponse.builder()
            .id(tenant.getId())
            .name(tenant.getName())
            .slug(tenant.getSlug())
            .build();
    }
}
