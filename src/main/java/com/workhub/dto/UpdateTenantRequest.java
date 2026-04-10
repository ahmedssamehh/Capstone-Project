package com.workhub.dto;

import com.workhub.entity.Tenant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTenantRequest {
    private String name;
    private String description;
    private Tenant.TenantStatus status;
    private Integer maxUsers;
    private Integer maxProjects;
}
