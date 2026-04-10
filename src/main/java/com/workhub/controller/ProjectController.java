package com.workhub.controller;

import com.workhub.dto.CreateProjectRequest;
import com.workhub.dto.ProjectDto;
import com.workhub.dto.UpdateProjectRequest;
import com.workhub.entity.Project;
import com.workhub.security.TenantContext;
import com.workhub.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<ProjectDto> createProject(@Valid @RequestBody CreateProjectRequest request) {
        ProjectDto project = projectService.createProject(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(project);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectDto> getProjectById(@PathVariable Long id) {
        ProjectDto project = projectService.getProjectById(id);
        return ResponseEntity.ok(project);
    }

    @GetMapping("/key/{projectKey}")
    public ResponseEntity<ProjectDto> getProjectByKey(@PathVariable String projectKey) {
        ProjectDto project = projectService.getProjectByKey(projectKey);
        return ResponseEntity.ok(project);
    }

    @GetMapping
    public ResponseEntity<List<ProjectDto>> getAllProjects() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        List<ProjectDto> projects = projectService.getAllProjectsByTenant(tenantId);
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<ProjectDto>> getProjectsByStatus(@PathVariable Project.ProjectStatus status) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        List<ProjectDto> projects = projectService.getProjectsByTenantAndStatus(tenantId, status);
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<ProjectDto>> getProjectsByOwner(@PathVariable Long ownerId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        List<ProjectDto> projects = projectService.getProjectsByTenantAndOwner(tenantId, ownerId);
        return ResponseEntity.ok(projects);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectDto> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProjectRequest request) {
        ProjectDto project = projectService.updateProject(id, request);
        return ResponseEntity.ok(project);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count")
    public ResponseEntity<Long> countProjects() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        long count = projectService.countProjectsByTenant(tenantId);
        return ResponseEntity.ok(count);
    }
}
