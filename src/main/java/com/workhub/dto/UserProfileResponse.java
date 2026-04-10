package com.workhub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * User profile response DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {

    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private String status;
    private Long tenantId;
    private String tenantName;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
}
