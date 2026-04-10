package com.workhub.service;

import com.workhub.dto.CreateUserRequest;
import com.workhub.dto.UpdateUserRequest;
import com.workhub.dto.UserDto;
import com.workhub.entity.User;

import java.util.List;
import java.util.UUID;

public interface UserService {
    
    UserDto createUser(CreateUserRequest request);
    
    UserDto getUserById(Long id);
    
    UserDto getUserByEmail(String email);
    
    List<UserDto> getAllUsersByTenant(UUID tenantId);
    
    List<UserDto> getUsersByTenantAndStatus(UUID tenantId, User.UserStatus status);
    
    List<UserDto> getUsersByTenantAndRole(UUID tenantId, User.UserRole role);
    
    UserDto updateUser(Long id, UpdateUserRequest request);
    
    void deleteUser(Long id);
    
    boolean existsByEmail(String email);
    
    long countUsersByTenant(UUID tenantId);
}
