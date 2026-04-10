package com.workhub.controller;

import com.workhub.dto.LoginRequest;
import com.workhub.dto.LoginResponse;
import com.workhub.dto.UserDto;
import com.workhub.dto.UserProfileResponse;
import com.workhub.security.CustomUserDetails;
import com.workhub.security.JwtUtil;
import com.workhub.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Authentication Controller
 * 
 * Handles user authentication and profile endpoints
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserService userService;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    /**
     * Login endpoint
     * 
     * POST /api/auth/login
     * 
     * @param loginRequest Login credentials
     * @return JWT token and user info
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Login attempt for email: {}", loginRequest.getEmail());

        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            // Get user details
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            // Update last login time
            userService.updateLastLogin(userDetails.getId());

            // Generate JWT token
            String token = jwtUtil.generateToken(
                    userDetails.getEmail(),
                    userDetails.getId(),
                    userDetails.getTenantId(),
                    userDetails.getRole()
            );

            // Build response
            LoginResponse response = LoginResponse.builder()
                    .token(token)
                    .type("Bearer")
                    .userId(userDetails.getId())
                    .email(userDetails.getEmail())
                    .tenantId(userDetails.getTenantId())
                    .role(userDetails.getRole())
                    .expiresIn(jwtExpiration)
                    .build();

            log.info("Login successful for user: {} (tenant: {})", userDetails.getEmail(), userDetails.getTenantId());

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            log.warn("Failed login attempt for email: {}", loginRequest.getEmail());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
    }

    /**
     * Get current user profile
     * 
     * GET /api/auth/me
     * 
     * @return Current user profile
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        
        // Load full user details through service (with tenant validation)
        UserDto user = userService.getUserById(userDetails.getId());

        // Build response
        UserProfileResponse response = UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .status(user.getStatus())
                .tenantId(user.getTenantId())
                .tenantName(user.getTenantName())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();

        return ResponseEntity.ok(response);
    }
}
