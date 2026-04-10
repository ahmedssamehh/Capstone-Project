package com.workhub.service;

import com.workhub.dto.CreateProjectRequest;
import com.workhub.dto.ProjectDto;
import com.workhub.dto.UpdateProjectRequest;
import com.workhub.entity.Project;

import java.util.List;
import java.util.UUID;

public interface ProjectService {
    
    ProjectDto createProject(CreateProjectRequest request);
    
    ProjectDto getProjectById(Long id);
    
    ProjectDto getProjectByKey(String projectKey);
    
    List<ProjectDto> getAllProjectsByTenant(UUID tenantId);
    
    List<ProjectDto> getProjectsByTenantAndStatus(UUID tenantId, Project.ProjectStatus status);
    
    List<ProjectDto> getProjectsByTenantAndOwner(UUID tenantId, Long ownerId);
    
    ProjectDto updateProject(Long id, UpdateProjectRequest request);
    
    void deleteProject(Long id);
    
    boolean existsByProjectKey(String projectKey);
    
    long countProjectsByTenant(UUID tenantId);
}
