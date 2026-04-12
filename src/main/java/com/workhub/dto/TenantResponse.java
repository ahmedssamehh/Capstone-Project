package com.workhub.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TenantResponse {
    Long id;
    String name;
    String slug;
}
