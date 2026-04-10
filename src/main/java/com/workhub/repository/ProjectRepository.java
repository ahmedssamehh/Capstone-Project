package com.workhub.repository;

import com.workhub.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Project Repository - Data access layer for Project entity
 * 
 * ALL QUERIES FILTER BY TENANT ID for multi-tenant isolation.
 * This ensures projects are never leaked across tenants.
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    // ========================================
    // TENANT-FILTERED QUERIES (REQUIRED)
    // ========================================

    /**
     * Find all projects by tenant ID
     * 
     * ALWAYS use this instead of findAll() to ensure tenant isolation
     * 
     * @param tenantId Tenant ID
     * @return List of projects in the tenant
     */
    @Query("SELECT p FROM Project p WHERE p.tenant.id = :tenantId")
    List<Project> findByTenantId(@Param("tenantId") Long tenantId);

    /**
     * Find project by ID and tenant ID
     * 
     * ALWAYS use this instead of findById() to ensure tenant isolation
     * 
     * @param id Project ID
     * @param tenantId Tenant ID
     * @return Optional containing project if found in tenant
     */
    @Query("SELECT p FROM Project p WHERE p.id = :id AND p.tenant.id = :tenantId")
    Optional<Project> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * Check if project exists by ID and tenant ID
     * 
     * @param id Project ID
     * @param tenantId Tenant ID
     * @return true if project exists in tenant
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
           "FROM Project p WHERE p.id = :id AND p.tenant.id = :tenantId")
    boolean existsByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * Delete project by ID and tenant ID
     * 
     * ALWAYS use this instead of deleteById() to ensure tenant isolation
     * 
     * @param id Project ID
     * @param tenantId Tenant ID
     */
    @Query("DELETE FROM Project p WHERE p.id = :id AND p.tenant.id = :tenantId")
    void deleteByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    // ========================================
    // STATUS-BASED QUERIES (TENANT-FILTERED)
    // ========================================

    /**
     * Find projects by tenant ID and status
     * 
     * @param tenantId Tenant ID
     * @param status Project status
     * @return List of projects with the specified status
     */
    @Query("SELECT p FROM Project p WHERE p.tenant.id = :tenantId AND p.status = :status")
    List<Project> findByTenantIdAndStatus(
        @Param("tenantId") Long tenantId, 
        @Param("status") Project.ProjectStatus status
    );

    /**
     * Find active projects in tenant
     * 
     * @param tenantId Tenant ID
     * @return List of active projects
     */
    default List<Project> findActiveProjectsByTenantId(Long tenantId) {
        return findByTenantIdAndStatus(tenantId, Project.ProjectStatus.ACTIVE);
    }

    /**
     * Find completed projects in tenant
     * 
     * @param tenantId Tenant ID
     * @return List of completed projects
     */
    default List<Project> findCompletedProjectsByTenantId(Long tenantId) {
        return findByTenantIdAndStatus(tenantId, Project.ProjectStatus.COMPLETED);
    }

    /**
     * Find archived projects in tenant
     * 
     * @param tenantId Tenant ID
     * @return List of archived projects
     */
    default List<Project> findArchivedProjectsByTenantId(Long tenantId) {
        return findByTenantIdAndStatus(tenantId, Project.ProjectStatus.ARCHIVED);
    }

    // ========================================
    // CREATOR-BASED QUERIES (TENANT-FILTERED)
    // ========================================

    /**
     * Find projects by tenant ID and creator
     * 
     * @param tenantId Tenant ID
     * @param createdById Creator user ID
     * @return List of projects created by the user
     */
    @Query("SELECT p FROM Project p WHERE p.tenant.id = :tenantId AND p.createdBy.id = :createdById")
    List<Project> findByTenantIdAndCreatedById(
        @Param("tenantId") Long tenantId, 
        @Param("createdById") Long createdById
    );

    /**
     * Find projects by tenant ID, creator, and status
     * 
     * @param tenantId Tenant ID
     * @param createdById Creator user ID
     * @param status Project status
     * @return List of matching projects
     */
    @Query("SELECT p FROM Project p WHERE p.tenant.id = :tenantId " +
           "AND p.createdBy.id = :createdById AND p.status = :status")
    List<Project> findByTenantIdAndCreatedByIdAndStatus(
        @Param("tenantId") Long tenantId,
        @Param("createdById") Long createdById,
        @Param("status") Project.ProjectStatus status
    );

    // ========================================
    // SEARCH QUERIES (TENANT-FILTERED)
    // ========================================

    /**
     * Search projects by name in tenant
     * 
     * @param tenantId Tenant ID
     * @param name Name search string
     * @return List of matching projects
     */
    @Query("SELECT p FROM Project p WHERE p.tenant.id = :tenantId " +
           "AND LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Project> searchByNameInTenant(@Param("tenantId") Long tenantId, @Param("name") String name);

    /**
     * Find project by project key and tenant ID
     * 
     * @param tenantId Tenant ID
     * @param projectKey Project key
     * @return Optional containing project if found
     */
    @Query("SELECT p FROM Project p WHERE p.tenant.id = :tenantId AND p.projectKey = :projectKey")
    Optional<Project> findByTenantIdAndProjectKey(
        @Param("tenantId") Long tenantId, 
        @Param("projectKey") String projectKey
    );

    /**
     * Search projects by description in tenant
     * 
     * @param tenantId Tenant ID
     * @param description Description search string
     * @return List of matching projects
     */
    @Query("SELECT p FROM Project p WHERE p.tenant.id = :tenantId " +
           "AND LOWER(p.description) LIKE LOWER(CONCAT('%', :description, '%'))")
    List<Project> searchByDescriptionInTenant(
        @Param("tenantId") Long tenantId, 
        @Param("description") String description
    );

    // ========================================
    // DATE-BASED QUERIES (TENANT-FILTERED)
    // ========================================

    /**
     * Find projects created after specified date
     * 
     * @param tenantId Tenant ID
     * @param date Date threshold
     * @return List of projects
     */
    @Query("SELECT p FROM Project p WHERE p.tenant.id = :tenantId AND p.createdAt > :date")
    List<Project> findProjectsCreatedAfter(@Param("tenantId") Long tenantId, @Param("date") LocalDateTime date);

    /**
     * Find projects with end date before specified date
     * 
     * @param tenantId Tenant ID
     * @param date Date threshold
     * @return List of projects
     */
    @Query("SELECT p FROM Project p WHERE p.tenant.id = :tenantId " +
           "AND p.endDate IS NOT NULL AND p.endDate < :date")
    List<Project> findProjectsEndingBefore(@Param("tenantId") Long tenantId, @Param("date") LocalDateTime date);

    /**
     * Find projects with start date between dates
     * 
     * @param tenantId Tenant ID
     * @param startDate Start date
     * @param endDate End date
     * @return List of projects
     */
    @Query("SELECT p FROM Project p WHERE p.tenant.id = :tenantId " +
           "AND p.startDate BETWEEN :startDate AND :endDate")
    List<Project> findProjectsStartingBetween(
        @Param("tenantId") Long tenantId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    // ========================================
    // COUNT QUERIES (TENANT-FILTERED)
    // ========================================

    /**
     * Count projects by tenant ID
     * 
     * @param tenantId Tenant ID
     * @return Number of projects in tenant
     */
    @Query("SELECT COUNT(p) FROM Project p WHERE p.tenant.id = :tenantId")
    long countByTenantId(@Param("tenantId") Long tenantId);

    /**
     * Count projects by tenant ID and status
     * 
     * @param tenantId Tenant ID
     * @param status Project status
     * @return Number of projects with the specified status
     */
    @Query("SELECT COUNT(p) FROM Project p WHERE p.tenant.id = :tenantId AND p.status = :status")
    long countByTenantIdAndStatus(
        @Param("tenantId") Long tenantId, 
        @Param("status") Project.ProjectStatus status
    );

    /**
     * Count projects by tenant ID and creator
     * 
     * @param tenantId Tenant ID
     * @param createdById Creator user ID
     * @return Number of projects created by the user
     */
    @Query("SELECT COUNT(p) FROM Project p WHERE p.tenant.id = :tenantId AND p.createdBy.id = :createdById")
    long countByTenantIdAndCreatedById(@Param("tenantId") Long tenantId, @Param("createdById") Long createdById);

    // ========================================
    // VALIDATION QUERIES (TENANT-FILTERED)
    // ========================================

    /**
     * Check if project key exists in tenant
     * 
     * @param tenantId Tenant ID
     * @param projectKey Project key
     * @return true if project key exists in tenant
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
           "FROM Project p WHERE p.tenant.id = :tenantId AND p.projectKey = :projectKey")
    boolean existsByProjectKeyInTenant(
        @Param("tenantId") Long tenantId, 
        @Param("projectKey") String projectKey
    );

    /**
     * Check if project key exists in tenant (excluding specific project)
     * Useful for update operations
     * 
     * @param tenantId Tenant ID
     * @param projectKey Project key
     * @param excludeProjectId Project ID to exclude from check
     * @return true if project key exists
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
           "FROM Project p WHERE p.tenant.id = :tenantId " +
           "AND p.projectKey = :projectKey AND p.id != :excludeProjectId")
    boolean existsByProjectKeyInTenantExcludingProject(
        @Param("tenantId") Long tenantId,
        @Param("projectKey") String projectKey,
        @Param("excludeProjectId") Long excludeProjectId
    );

    // ========================================
    // STATISTICS QUERIES (TENANT-FILTERED)
    // ========================================

    /**
     * Find project with tasks loaded
     * 
     * @param id Project ID
     * @param tenantId Tenant ID
     * @return Optional containing project with tasks
     */
    @Query("SELECT p FROM Project p LEFT JOIN FETCH p.tasks " +
           "WHERE p.id = :id AND p.tenant.id = :tenantId")
    Optional<Project> findByIdAndTenantIdWithTasks(
        @Param("id") Long id, 
        @Param("tenantId") Long tenantId
    );

    /**
     * Find projects with task count greater than specified value
     * 
     * @param tenantId Tenant ID
     * @param count Minimum task count
     * @return List of projects
     */
    @Query("SELECT p FROM Project p WHERE p.tenant.id = :tenantId AND SIZE(p.tasks) > :count")
    List<Project> findProjectsWithTaskCountGreaterThan(
        @Param("tenantId") Long tenantId, 
        @Param("count") int count
    );
}
