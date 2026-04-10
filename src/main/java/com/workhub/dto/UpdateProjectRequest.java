package com.workhub.dto;

import com.workhub.entity.Project;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProjectRequest {
    private String name;
    private String description;
    private Project.ProjectStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long ownerId;
}
