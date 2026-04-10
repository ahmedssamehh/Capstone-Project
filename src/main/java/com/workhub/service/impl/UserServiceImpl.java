package com.workhub.service.impl;

import com.workhub.dto.CreateUserRequest;
import com.workhub.dto.UpdateUserRequest;
import com.workhub.dto.UserDto;
import com.workhub.entity.Tenant;
import com.workhub.entity.User;
import com.workhub.repository.TenantRepository;
import com.workhub.repository.UserRepository;
import com.workhub.security.TenantContext;
import com.workhub.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * User Service Implementation
 * 
 * Business logic layer for User operations with strict tenant isolation.
 * All operations automatically use tenantId from TenantContext.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDto createUser(CreateUserRequest request) {
        Long tenantId = getTenantId();
        log.info("Creating user '{}' for tenant: {}", request.getEmail(), tenantId);

        // Validate email uniqueness
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }

        // Get tenant
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));

        // Create user
        User user = User.builder()
                .tenant(tenant)
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(request.getRole() != null ? request.getRole() : User.UserRole.TENANT_USER)
                .status(User.UserStatus.ACTIVE)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Created user {} for tenant: {}", savedUser.getId(), tenantId);

        return UserDto.fromEntity(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        Long tenantId = getTenantId();
        log.debug("Fetching user {} for tenant: {}", id, tenantId);

        User user = userRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("User not found or access denied: " + id));

        return UserDto.fromEntity(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUserByEmail(String email) {
        Long tenantId = getTenantId();
        log.debug("Fetching user by email '{}' for tenant: {}", email, tenantId);

        User user = userRepository.findByEmailAndTenantId(email, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));

        return UserDto.fromEntity(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getAllUsersByTenant() {
        Long tenantId = getTenantId();
        log.debug("Fetching all users for tenant: {}", tenantId);

        List<User> users = userRepository.findByTenantId(tenantId);
        return users.stream()
                .map(UserDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getUsersByTenantAndStatus(User.UserStatus status) {
        Long tenantId = getTenantId();
        log.debug("Fetching users with status {} for tenant: {}", status, tenantId);

        List<User> users = userRepository.findByTenantIdAndStatus(tenantId, status);
        return users.stream()
                .map(UserDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getUsersByTenantAndRole(User.UserRole role) {
        Long tenantId = getTenantId();
        log.debug("Fetching users with role {} for tenant: {}", role, tenantId);

        List<User> users = userRepository.findByTenantIdAndRole(tenantId, role);
        return users.stream()
                .map(UserDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public UserDto updateUser(Long id, UpdateUserRequest request) {
        Long tenantId = getTenantId();
        log.info("Updating user {} for tenant: {}", id, tenantId);

        // Get existing user with tenant validation
        User user = userRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("User not found or access denied: " + id));

        // Update fields
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            // Check email uniqueness
            if (userRepository.existsByEmailInTenantExcludingUser(request.getEmail(), tenantId, id)) {
                throw new IllegalArgumentException("Email already exists: " + request.getEmail());
            }
            user.setEmail(request.getEmail());
        }
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }

        User savedUser = userRepository.save(user);
        log.info("Updated user {} for tenant: {}", id, tenantId);

        return UserDto.fromEntity(savedUser);
    }

    @Override
    public void deleteUser(Long id) {
        Long tenantId = getTenantId();
        log.info("Deleting user {} for tenant: {}", id, tenantId);

        // Verify user exists and belongs to tenant
        User user = userRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("User not found or access denied: " + id));

        userRepository.delete(user);
        log.info("Deleted user {} for tenant: {}", id, tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUsersByTenant() {
        Long tenantId = getTenantId();
        log.debug("Counting users for tenant: {}", tenantId);
        return userRepository.countByTenantId(tenantId);
    }

    @Override
    public void updateLastLogin(Long userId) {
        Long tenantId = getTenantId();
        log.debug("Updating last login for user {} in tenant: {}", userId, tenantId);

        userRepository.findByIdAndTenantId(userId, tenantId).ifPresent(user -> {
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
            log.debug("Updated last login for user {}", userId);
        });
    }

    /**
     * Get current tenant ID from context
     * 
     * @return Tenant ID
     * @throws IllegalStateException if tenant context not set
     */
    private Long getTenantId() {
        return TenantContext.getRequiredTenantId();
    }
}
