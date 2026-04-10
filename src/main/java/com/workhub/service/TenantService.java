package com.workhub.service;

import com.workhub.dto.CreateTenantRequest;
import com.workhub.dto.TenantResponse;

import java.util.List;

public interface TenantService {
    TenantResponse createTenant(CreateTenantRequest request);

    List<TenantResponse> listTenants();
}
