package com.workhub.service;

import com.workhub.dto.CreateProjectRequest;
import com.workhub.dto.ProjectDto;
import com.workhub.dto.UpdateProjectRequest;
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

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;

    @Override
    public ProjectDto createProject(CreateProjectRequest request) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        log.info("Creating project with key: {} for tenant: {}", request.getProjectKey(), tenantId);

        if (projectRepository.existsByProjectKey(request.getProjectKey())) {
            throw new IllegalArgumentException("Project with key already exists: " + request.getProjectKey());
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));

        Project.ProjectBuilder projectBuilder = Project.builder()
                .tenant(tenant)
                .name(request.getName())
                .description(request.getDescription())
                .projectKey(request.getProjectKey())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(Project.ProjectStatus.ACTIVE);

        if (request.getOwnerId() != null) {
            User owner = userRepository.findById(request.getOwnerId())
                    .orElseThrow(() -> new IllegalArgumentException("Owner not found: " + request.getOwnerId()));
            projectBuilder.owner(owner);
        }

        Project savedProject = projectRepository.save(projectBuilder.build());
        return mapToDto(savedProject);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectDto getProjectById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + id));
        
        validateTenantAccess(project.getTenant().getId());
        return mapToDto(project);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectDto getProjectByKey(String projectKey) {
        Project project = projectRepository.findByProjectKey(projectKey)
                .orElseThrow(() -> new IllegalArgumentException("Project not found with key: " + projectKey));
        
        validateTenantAccess(project.getTenant().getId());
        return mapToDto(project);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectDto> getAllProjectsByTenant(UUID tenantId) {
        validateTenantAccess(tenantId);
        return projectRepository.findByTenantId(tenantId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectDto> getProjectsByTenantAndStatus(UUID tenantId, Project.ProjectStatus status) {
        validateTenantAccess(tenantId);
        return projectRepository.findByTenantIdAndStatus(tenantId, status).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectDto> getProjectsByTenantAndOwner(UUID tenantId, Long ownerId) {
        validateTenantAccess(tenantId);
        return projectRepository.findByTenantIdAndOwnerId(tenantId, ownerId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public ProjectDto updateProject(Long id, UpdateProjectRequest request) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + id));
        
        validateTenantAccess(project.getTenant().getId());

        if (request.getName() != null) {
            project.setName(request.getName());
        }
        if (request.getDescription() != null) {
            project.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            project.setStatus(request.getStatus());
        }
        if (request.getStartDate() != null) {
            project.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            project.setEndDate(request.getEndDate());
        }
        if (request.getOwnerId() != null) {
            User owner = userRepository.findById(request.getOwnerId())
                    .orElseThrow(() -> new IllegalArgumentException("Owner not found: " + request.getOwnerId()));
            project.setOwner(owner);
        }

        Project updatedProject = projectRepository.save(project);
        return mapToDto(updatedProject);
    }

    @Override
    public void deleteProject(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + id));
        
        validateTenantAccess(project.getTenant().getId());
        projectRepository.delete(project);
        log.info("Deleted project: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByProjectKey(String projectKey) {
        return projectRepository.existsByProjectKey(projectKey);
    }

    @Override
    @Transactional(readOnly = true)
    public long countProjectsByTenant(UUID tenantId) {
        validateTenantAccess(tenantId);
        return projectRepository.countByTenantId(tenantId);
    }

    private void validateTenantAccess(UUID tenantId) {
        UUID currentTenantId = TenantContext.getCurrentTenantId();
        if (!tenantId.equals(currentTenantId)) {
            throw new SecurityException("Access denied to tenant: " + tenantId);
        }
    }

    private ProjectDto mapToDto(Project project) {
        return ProjectDto.builder()
                .id(project.getId())
                .tenantId(project.getTenant().getId())
                .name(project.getName())
                .description(project.getDescription())
                .projectKey(project.getProjectKey())
                .status(project.getStatus())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .ownerId(project.getOwner() != null ? project.getOwner().getId() : null)
                .ownerName(project.getOwner() != null ? 
                        project.getOwner().getFirstName() + " " + project.getOwner().getLastName() : null)
                .taskCount(project.getTasks() != null ? project.getTasks().size() : 0)
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}
