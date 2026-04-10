package com.workhub.dto;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class TenantResponse {
    UUID id;
    String name;
    String slug;
}
