package com.workhub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Login response DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {

    private String token;
    private String type = "Bearer";
    private Long userId;
    private String email;
    private Long tenantId;
    private String role;
    private Long expiresIn;

    public LoginResponse(String token, Long userId, String email, Long tenantId, String role, Long expiresIn) {
        this.token = token;
        this.userId = userId;
        this.email = email;
        this.tenantId = tenantId;
        this.role = role;
        this.expiresIn = expiresIn;
    }
}
