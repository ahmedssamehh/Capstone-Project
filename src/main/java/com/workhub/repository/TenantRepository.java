package com.workhub.repository;

import com.workhub.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Tenant Repository - Data access layer for Tenant entity
 * 
 * Provides CRUD operations and custom queries for tenants.
 * This is the root repository for multi-tenant operations.
 */
@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    /**
     * Find tenant by name
     * 
     * @param name Tenant name
     * @return Optional containing tenant if found
     */
    Optional<Tenant> findByName(String name);

    /**
     * Find tenant by name (case-insensitive)
     * 
     * @param name Tenant name
     * @return Optional containing tenant if found
     */
    Optional<Tenant> findByNameIgnoreCase(String name);

    /**
     * Find tenants by plan
     * 
     * @param plan Tenant plan (FREE, STARTER, PROFESSIONAL, ENTERPRISE)
     * @return List of tenants with the specified plan
     */
    List<Tenant> findByPlan(Tenant.TenantPlan plan);

    /**
     * Find tenants by status
     * 
     * @param status Tenant status (ACTIVE, SUSPENDED, TRIAL, EXPIRED)
     * @return List of tenants with the specified status
     */
    List<Tenant> findByStatus(Tenant.TenantStatus status);

    /**
     * Find active tenants
     * 
     * @return List of active tenants
     */
    default List<Tenant> findActiveTenants() {
        return findByStatus(Tenant.TenantStatus.ACTIVE);
    }

    /**
     * Find tenants by plan and status
     * 
     * @param plan Tenant plan
     * @param status Tenant status
     * @return List of matching tenants
     */
    List<Tenant> findByPlanAndStatus(Tenant.TenantPlan plan, Tenant.TenantStatus status);

    /**
     * Search tenants by name containing string (case-insensitive)
     * 
     * @param name Name search string
     * @return List of matching tenants
     */
    List<Tenant> findByNameContainingIgnoreCase(String name);

    /**
     * Check if tenant exists by name
     * 
     * @param name Tenant name
     * @return true if tenant exists
     */
    boolean existsByName(String name);

    /**
     * Check if tenant exists by name (case-insensitive)
     * 
     * @param name Tenant name
     * @return true if tenant exists
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Count tenants by plan
     * 
     * @param plan Tenant plan
     * @return Number of tenants with the specified plan
     */
    long countByPlan(Tenant.TenantPlan plan);

    /**
     * Count tenants by status
     * 
     * @param status Tenant status
     * @return Number of tenants with the specified status
     */
    long countByStatus(Tenant.TenantStatus status);

    /**
     * Find tenants with user count greater than specified value
     * 
     * Custom query to find tenants based on their user count
     * 
     * @param count Minimum user count
     * @return List of tenants
     */
    @Query("SELECT t FROM Tenant t WHERE SIZE(t.users) > :count")
    List<Tenant> findTenantsWithUserCountGreaterThan(@Param("count") int count);

    /**
     * Find tenants with project count greater than specified value
     * 
     * @param count Minimum project count
     * @return List of tenants
     */
    @Query("SELECT t FROM Tenant t WHERE SIZE(t.projects) > :count")
    List<Tenant> findTenantsWithProjectCountGreaterThan(@Param("count") int count);

    /**
     * Get tenant statistics
     * 
     * Returns tenant with user and project counts
     * 
     * @param tenantId Tenant ID
     * @return Tenant with loaded collections
     */
    @Query("SELECT t FROM Tenant t " +
           "LEFT JOIN FETCH t.users " +
           "LEFT JOIN FETCH t.projects " +
           "WHERE t.id = :tenantId")
    Optional<Tenant> findByIdWithStatistics(@Param("tenantId") UUID tenantId);
}
