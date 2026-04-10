package com.workhub.service;

import com.workhub.entity.Project;
import com.workhub.entity.Tenant;
import com.workhub.entity.User;
import com.workhub.repository.ProjectRepository;
import com.workhub.repository.TenantRepository;
import com.workhub.repository.UserRepository;
import com.workhub.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Project Service
 * 
 * Business logic layer for Project operations.
 * 
 * TENANT ISOLATION GUARANTEE:
 * - All operations automatically use tenantId from TenantContext
 * - No cross-tenant access is possible
 * - All repository calls explicitly filter by tenantId
 * - Validation ensures entities belong to current tenant
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;

    // ========================================
    // READ OPERATIONS
    // ========================================

    /**
     * Get all projects for current tenant
     * 
     * @return List of projects
     */
    @Transactional(readOnly = true)
    public List<Project> getAllProjects() {
        Long tenantId = getTenantId();
        log.debug("Fetching all projects for tenant: {}", tenantId);
        return projectRepository.findByTenantId(tenantId);
    }

    /**
     * Get project by ID for current tenant
     * 
     * @param projectId Project ID
     * @return Project if found and belongs to current tenant
     */
    @Transactional(readOnly = true)
    public Optional<Project> getProjectById(Long projectId) {
        Long tenantId = getTenantId();
        log.debug("Fetching project {} for tenant: {}", projectId, tenantId);
        return projectRepository.findByIdAndTenantId(projectId, tenantId);
    }

    /**
     * Get project by ID or throw exception
     * 
     * @param projectId Project ID
     * @return Project
     * @throws IllegalArgumentException if project not found or doesn't belong to tenant
     */
    @Transactional(readOnly = true)
    public Project getProjectByIdOrThrow(Long projectId) {
        return getProjectById(projectId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Project not found or access denied: " + projectId));
    }

    /**
     * Get project with tasks loaded
     * 
     * @param projectId Project ID
     * @return Project with tasks
     */
    @Transactional(readOnly = true)
    public Optional<Project> getProjectWithTasks(Long projectId) {
        Long tenantId = getTenantId();
        log.debug("Fetching project {} with tasks for tenant: {}", projectId, tenantId);
        return projectRepository.findByIdAndTenantIdWithTasks(projectId, tenantId);
    }

    /**
     * Get projects by status
     * 
     * @param status Project status
     * @return List of projects
     */
    @Transactional(readOnly = true)
    public List<Project> getProjectsByStatus(Project.ProjectStatus status) {
        Long tenantId = getTenantId();
        log.debug("Fetching projects with status {} for tenant: {}", status, tenantId);
        return projectRepository.findByTenantIdAndStatus(tenantId, status);
    }

    /**
     * Get active projects
     * 
     * @return List of active projects
     */
    @Transactional(readOnly = true)
    public List<Project> getActiveProjects() {
        Long tenantId = getTenantId();
        return projectRepository.findActiveProjectsByTenantId(tenantId);
    }

    /**
     * Get projects created by user
     * 
     * @param userId User ID
     * @return List of projects
     */
    @Transactional(readOnly = true)
    public List<Project> getProjectsByCreator(Long userId) {
        Long tenantId = getTenantId();
        validateUserBelongsToTenant(userId, tenantId);
        return projectRepository.findByTenantIdAndCreatedById(tenantId, userId);
    }

    /**
     * Search projects by name
     * 
     * @param name Name search string
     * @return List of matching projects
     */
    @Transactional(readOnly = true)
    public List<Project> searchProjectsByName(String name) {
        Long tenantId = getTenantId();
        log.debug("Searching projects by name '{}' for tenant: {}", name, tenantId);
        return projectRepository.searchByNameInTenant(tenantId, name);
    }

    /**
     * Get project by project key
     * 
     * @param projectKey Project key
     * @return Project if found
     */
    @Transactional(readOnly = true)
    public Optional<Project> getProjectByKey(String projectKey) {
        Long tenantId = getTenantId();
        return projectRepository.findByTenantIdAndProjectKey(tenantId, projectKey);
    }

    // ========================================
    // CREATE OPERATIONS
    // ========================================

    /**
     * Create new project for current tenant
     * 
     * @param project Project to create
     * @param creatorId Creator user ID
     * @return Created project
     */
    public Project createProject(Project project, Long creatorId) {
        Long tenantId = getTenantId();
        log.info("Creating project '{}' for tenant: {}", project.getName(), tenantId);

        // Validate tenant isolation
        validateTenantIsolation(project, tenantId);
        
        // Validate creator belongs to tenant
        validateUserBelongsToTenant(creatorId, tenantId);

        // Validate project key uniqueness
        if (project.getProjectKey() != null) {
            validateProjectKeyUnique(project.getProjectKey(), tenantId);
        }

        // Set tenant and creator (with validation)
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));
        User creator = userRepository.findByIdAndTenantId(creatorId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Creator not found: " + creatorId));
        
        project.setTenant(tenant);
        project.setCreatedBy(creator);
        
        // Set default status if not provided
        if (project.getStatus() == null) {
            project.setStatus(Project.ProjectStatus.ACTIVE);
        }

        Project savedProject = projectRepository.save(project);
        log.info("Created project {} for tenant: {}", savedProject.getId(), tenantId);
        
        return savedProject;
    }

    // ========================================
    // UPDATE OPERATIONS
    // ========================================

    /**
     * Update project for current tenant
     * 
     * @param projectId Project ID
     * @param updatedProject Updated project data
     * @return Updated project
     */
    public Project updateProject(Long projectId, Project updatedProject) {
        Long tenantId = getTenantId();
        log.info("Updating project {} for tenant: {}", projectId, tenantId);

        // Fetch existing project (ensures tenant ownership)
        Project existingProject = getProjectByIdOrThrow(projectId);

        // Validate tenant isolation
        validateTenantIsolation(updatedProject, tenantId);

        // Validate project key uniqueness if changed
        if (updatedProject.getProjectKey() != null && 
            !updatedProject.getProjectKey().equals(existingProject.getProjectKey())) {
            validateProjectKeyUniqueExcludingProject(
                updatedProject.getProjectKey(), 
                tenantId, 
                projectId
            );
        }

        // Update fields
        existingProject.setName(updatedProject.getName());
        existingProject.setDescription(updatedProject.getDescription());
        existingProject.setProjectKey(updatedProject.getProjectKey());
        existingProject.setStatus(updatedProject.getStatus());
        existingProject.setStartDate(updatedProject.getStartDate());
        existingProject.setEndDate(updatedProject.getEndDate());

        Project savedProject = projectRepository.save(existingProject);
        log.info("Updated project {} for tenant: {}", projectId, tenantId);
        
        return savedProject;
    }

    /**
     * Update project status
     * 
     * @param projectId Project ID
     * @param status New status
     * @return Updated project
     */
    public Project updateProjectStatus(Long projectId, Project.ProjectStatus status) {
        Long tenantId = getTenantId();
        log.info("Updating project {} status to {} for tenant: {}", projectId, status, tenantId);

        Project project = getProjectByIdOrThrow(projectId);
        project.setStatus(status);

        return projectRepository.save(project);
    }

    // ========================================
    // DELETE OPERATIONS
    // ========================================

    /**
     * Delete project for current tenant
     * 
     * @param projectId Project ID
     * @return true if deleted, false if not found
     */
    public boolean deleteProject(Long projectId) {
        Long tenantId = getTenantId();
        log.info("Deleting project {} for tenant: {}", projectId, tenantId);

        if (projectRepository.existsByIdAndTenantId(projectId, tenantId)) {
            projectRepository.deleteByIdAndTenantId(projectId, tenantId);
            log.info("Deleted project {} for tenant: {}", projectId, tenantId);
            return true;
        }
        
        log.warn("Project {} not found for tenant: {}", projectId, tenantId);
        return false;
    }

    /**
     * Archive project (soft delete)
     * 
     * @param projectId Project ID
     * @return Archived project
     */
    public Project archiveProject(Long projectId) {
        return updateProjectStatus(projectId, Project.ProjectStatus.ARCHIVED);
    }

    // ========================================
    // STATISTICS & COUNTS
    // ========================================

    /**
     * Count projects for current tenant
     * 
     * @return Number of projects
     */
    @Transactional(readOnly = true)
    public long countProjects() {
        Long tenantId = getTenantId();
        return projectRepository.countByTenantId(tenantId);
    }

    /**
     * Count projects by status
     * 
     * @param status Project status
     * @return Number of projects
     */
    @Transactional(readOnly = true)
    public long countProjectsByStatus(Project.ProjectStatus status) {
        Long tenantId = getTenantId();
        return projectRepository.countByTenantIdAndStatus(tenantId, status);
    }

    /**
     * Count projects created by user
     * 
     * @param userId User ID
     * @return Number of projects
     */
    @Transactional(readOnly = true)
    public long countProjectsByCreator(Long userId) {
        Long tenantId = getTenantId();
        validateUserBelongsToTenant(userId, tenantId);
        return projectRepository.countByTenantIdAndCreatedById(tenantId, userId);
    }

    /**
     * Get projects created after date
     * 
     * @param date Date threshold
     * @return List of projects
     */
    @Transactional(readOnly = true)
    public List<Project> getProjectsCreatedAfter(LocalDateTime date) {
        Long tenantId = getTenantId();
        return projectRepository.findProjectsCreatedAfter(tenantId, date);
    }

    // ========================================
    // VALIDATION METHODS
    // ========================================

    /**
     * Check if project exists for current tenant
     * 
     * @param projectId Project ID
     * @return true if exists
     */
    @Transactional(readOnly = true)
    public boolean projectExists(Long projectId) {
        Long tenantId = getTenantId();
        return projectRepository.existsByIdAndTenantId(projectId, tenantId);
    }

    /**
     * Check if project key exists
     * 
     * @param projectKey Project key
     * @return true if exists
     */
    @Transactional(readOnly = true)
    public boolean projectKeyExists(String projectKey) {
        Long tenantId = getTenantId();
        return projectRepository.existsByProjectKeyInTenant(tenantId, projectKey);
    }

    // ========================================
    // PRIVATE HELPER METHODS
    // ========================================

    /**
     * Get current tenant ID from context
     * 
     * @return Tenant ID
     * @throws IllegalStateException if tenant context not set
     */
    private Long getTenantId() {
        return TenantContext.getRequiredTenantId();
    }

    /**
     * Validate that project belongs to current tenant
     * Prevents cross-tenant data manipulation
     * 
     * @param project Project to validate
     * @param tenantId Expected tenant ID
     * @throws IllegalArgumentException if tenant mismatch
     */
    private void validateTenantIsolation(Project project, Long tenantId) {
        if (project.getTenantId() != null && !project.getTenantId().equals(tenantId)) {
            log.error("Tenant isolation violation: project tenant {} != context tenant {}", 
                     project.getTenantId(), tenantId);
            throw new IllegalArgumentException(
                "Project tenant ID does not match current tenant context");
        }
    }

    /**
     * Validate that user belongs to current tenant
     * 
     * @param userId User ID
     * @param tenantId Tenant ID
     * @throws IllegalArgumentException if user doesn't belong to tenant
     */
    private void validateUserBelongsToTenant(Long userId, Long tenantId) {
        if (!userRepository.findByIdAndTenantId(userId, tenantId).isPresent()) {
            log.error("User {} does not belong to tenant {}", userId, tenantId);
            throw new IllegalArgumentException(
                "User does not belong to current tenant");
        }
    }

    /**
     * Validate project key is unique in tenant
     * 
     * @param projectKey Project key
     * @param tenantId Tenant ID
     * @throws IllegalArgumentException if project key exists
     */
    private void validateProjectKeyUnique(String projectKey, Long tenantId) {
        if (projectRepository.existsByProjectKeyInTenant(tenantId, projectKey)) {
            throw new IllegalArgumentException(
                "Project key already exists: " + projectKey);
        }
    }

    /**
     * Validate project key is unique excluding specific project
     * 
     * @param projectKey Project key
     * @param tenantId Tenant ID
     * @param excludeProjectId Project ID to exclude
     * @throws IllegalArgumentException if project key exists
     */
    private void validateProjectKeyUniqueExcludingProject(
            String projectKey, Long tenantId, Long excludeProjectId) {
        if (projectRepository.existsByProjectKeyInTenantExcludingProject(
                tenantId, projectKey, excludeProjectId)) {
            throw new IllegalArgumentException(
                "Project key already exists: " + projectKey);
        }
    }
}
