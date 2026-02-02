package com.ns.selai.testmanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDTO {

    private Long id;

    @NotBlank(message = "Project name is required")
    @Size(min = 3, max = 255, message = "Project name must be between 3 and 255 characters")
    private String name;

    @NotBlank(message = "URL is required")
    @Size(max = 500, message = "URL must not exceed 500 characters")
    private String url;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    private String createdBy;

    private String browserType; // chrome, firefox, edge

    private String testType; // smoke, regression, functional

    private String createdAt;

    private String updatedAt;

    private Boolean isActive;
}
