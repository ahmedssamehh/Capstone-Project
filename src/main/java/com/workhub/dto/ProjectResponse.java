package com.workhub.dto;

import com.workhub.entity.Project;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for project response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectResponse {

    private Long id;
    private String name;
    private String description;
    private String projectKey;
    private String status;
    private Long tenantId;
    private String tenantName;
    private Long createdById;
    private String createdByName;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer taskCount;

    /**
     * Convert Project entity to ProjectResponse DTO
     */
    public static ProjectResponse fromEntity(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .projectKey(project.getProjectKey())
                .status(project.getStatus().name())
                .tenantId(project.getTenantId())
                .tenantName(project.getTenant().getName())
                .createdById(project.getCreatedBy().getId())
                .createdByName(project.getCreatedBy().getFirstName() + " " + project.getCreatedBy().getLastName())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .taskCount(project.getTasks() != null ? project.getTasks().size() : 0)
                .build();
    }
}
