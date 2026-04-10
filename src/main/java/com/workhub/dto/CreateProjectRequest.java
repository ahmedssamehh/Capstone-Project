package com.workhub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateProjectRequest {

    @NotBlank(message = "Project name is required")
    private String name;

    private String description;

    @NotBlank(message = "Project key is required")
    @Pattern(regexp = "^[A-Z]{2,5}$", message = "Project key must be 2-5 uppercase letters")
    private String projectKey;

    private LocalDate startDate;
    private LocalDate endDate;
    private Long ownerId;
}
