package com.workhub.dto;

import com.workhub.entity.Project;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectDto {
    private Long id;
    private UUID tenantId;
    private String name;
    private String description;
    private String projectKey;
    private Project.ProjectStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long ownerId;
    private String ownerName;
    private Integer taskCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
