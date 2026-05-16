package com.workhub.service;

import com.workhub.dto.CreateTenantRequest;
import com.workhub.dto.TenantResponse;

public interface TenantService {
    TenantResponse createTenant(CreateTenantRequest request);
    TenantResponse getCurrentTenant();
}
