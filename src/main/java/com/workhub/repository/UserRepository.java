package com.workhub.repository;

import com.workhub.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * User Repository - Data access layer for User entity
 * 
 * Provides CRUD operations and tenant-aware queries for users.
 * All queries should consider tenant context for multi-tenant isolation.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // ========================================
    // BASIC QUERIES
    // ========================================

    /**
     * Find user by email
     * 
     * @param email User email
     * @return Optional containing user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by email (case-insensitive)
     * 
     * @param email User email
     * @return Optional containing user if found
     */
    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Check if user exists by email
     * 
     * @param email User email
     * @return true if user exists
     */
    boolean existsByEmail(String email);

    // ========================================
    // TENANT-SCOPED QUERIES
    // ========================================

    /**
     * Find all users by tenant ID
     * 
     * @param tenantId Tenant ID
     * @return List of users in the tenant
     */
    @Query("SELECT u FROM User u WHERE u.tenant.id = :tenantId")
    List<User> findByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Find user by ID and tenant ID
     * Ensures user belongs to the specified tenant
     * 
     * @param id User ID
     * @param tenantId Tenant ID
     * @return Optional containing user if found in tenant
     */
    @Query("SELECT u FROM User u WHERE u.id = :id AND u.tenant.id = :tenantId")
    Optional<User> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") UUID tenantId);

    /**
     * Find user by email and tenant ID
     * 
     * @param email User email
     * @param tenantId Tenant ID
     * @return Optional containing user if found
     */
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.tenant.id = :tenantId")
    Optional<User> findByEmailAndTenantId(@Param("email") String email, @Param("tenantId") UUID tenantId);

    // ========================================
    // ROLE-BASED QUERIES
    // ========================================

    /**
     * Find users by tenant ID and role
     * 
     * @param tenantId Tenant ID
     * @param role User role
     * @return List of users with the specified role
     */
    @Query("SELECT u FROM User u WHERE u.tenant.id = :tenantId AND u.role = :role")
    List<User> findByTenantIdAndRole(@Param("tenantId") UUID tenantId, @Param("role") User.UserRole role);

    /**
     * Find tenant admins
     * 
     * @param tenantId Tenant ID
     * @return List of admin users
     */
    default List<User> findTenantAdmins(UUID tenantId) {
        return findByTenantIdAndRole(tenantId, User.UserRole.TENANT_ADMIN);
    }

    /**
     * Find tenant users (non-admins)
     * 
     * @param tenantId Tenant ID
     * @return List of regular users
     */
    default List<User> findTenantUsers(UUID tenantId) {
        return findByTenantIdAndRole(tenantId, User.UserRole.TENANT_USER);
    }

    // ========================================
    // STATUS-BASED QUERIES
    // ========================================

    /**
     * Find users by tenant ID and status
     * 
     * @param tenantId Tenant ID
     * @param status User status
     * @return List of users with the specified status
     */
    @Query("SELECT u FROM User u WHERE u.tenant.id = :tenantId AND u.status = :status")
    List<User> findByTenantIdAndStatus(@Param("tenantId") UUID tenantId, @Param("status") User.UserStatus status);

    /**
     * Find active users in tenant
     * 
     * @param tenantId Tenant ID
     * @return List of active users
     */
    default List<User> findActiveUsersByTenantId(UUID tenantId) {
        return findByTenantIdAndStatus(tenantId, User.UserStatus.ACTIVE);
    }

    /**
     * Find inactive users in tenant
     * 
     * @param tenantId Tenant ID
     * @return List of inactive users
     */
    default List<User> findInactiveUsersByTenantId(UUID tenantId) {
        return findByTenantIdAndStatus(tenantId, User.UserStatus.INACTIVE);
    }

    // ========================================
    // SEARCH QUERIES
    // ========================================

    /**
     * Search users by name in tenant
     * 
     * @param tenantId Tenant ID
     * @param name Name search string
     * @return List of matching users
     */
    @Query("SELECT u FROM User u WHERE u.tenant.id = :tenantId " +
           "AND (LOWER(u.firstName) LIKE LOWER(CONCAT('%', :name, '%')) " +
           "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :name, '%')))")
    List<User> searchByNameInTenant(@Param("tenantId") UUID tenantId, @Param("name") String name);

    /**
     * Search users by email in tenant
     * 
     * @param tenantId Tenant ID
     * @param email Email search string
     * @return List of matching users
     */
    @Query("SELECT u FROM User u WHERE u.tenant.id = :tenantId " +
           "AND LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%'))")
    List<User> searchByEmailInTenant(@Param("tenantId") UUID tenantId, @Param("email") String email);

    // ========================================
    // COUNT QUERIES
    // ========================================

    /**
     * Count users by tenant ID
     * 
     * @param tenantId Tenant ID
     * @return Number of users in tenant
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.tenant.id = :tenantId")
    long countByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Count users by tenant ID and role
     * 
     * @param tenantId Tenant ID
     * @param role User role
     * @return Number of users with the specified role
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.tenant.id = :tenantId AND u.role = :role")
    long countByTenantIdAndRole(@Param("tenantId") UUID tenantId, @Param("role") User.UserRole role);

    /**
     * Count users by tenant ID and status
     * 
     * @param tenantId Tenant ID
     * @param status User status
     * @return Number of users with the specified status
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.tenant.id = :tenantId AND u.status = :status")
    long countByTenantIdAndStatus(@Param("tenantId") UUID tenantId, @Param("status") User.UserStatus status);

    // ========================================
    // ACTIVITY QUERIES
    // ========================================

    /**
     * Find users who logged in after specified date
     * 
     * @param tenantId Tenant ID
     * @param date Date threshold
     * @return List of users
     */
    @Query("SELECT u FROM User u WHERE u.tenant.id = :tenantId AND u.lastLoginAt > :date")
    List<User> findUsersLoggedInAfter(@Param("tenantId") UUID tenantId, @Param("date") LocalDateTime date);

    /**
     * Find users who never logged in
     * 
     * @param tenantId Tenant ID
     * @return List of users
     */
    @Query("SELECT u FROM User u WHERE u.tenant.id = :tenantId AND u.lastLoginAt IS NULL")
    List<User> findUsersNeverLoggedIn(@Param("tenantId") UUID tenantId);

    // ========================================
    // VALIDATION QUERIES
    // ========================================

    /**
     * Check if email exists in tenant
     * 
     * @param email User email
     * @param tenantId Tenant ID
     * @return true if email exists in tenant
     */
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END " +
           "FROM User u WHERE u.email = :email AND u.tenant.id = :tenantId")
    boolean existsByEmailInTenant(@Param("email") String email, @Param("tenantId") UUID tenantId);

    /**
     * Check if email exists in tenant (excluding specific user)
     * Useful for update operations
     * 
     * @param email User email
     * @param tenantId Tenant ID
     * @param excludeUserId User ID to exclude from check
     * @return true if email exists
     */
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END " +
           "FROM User u WHERE u.email = :email AND u.tenant.id = :tenantId AND u.id != :excludeUserId")
    boolean existsByEmailInTenantExcludingUser(
        @Param("email") String email, 
        @Param("tenantId") UUID tenantId, 
        @Param("excludeUserId") Long excludeUserId
    );
}
