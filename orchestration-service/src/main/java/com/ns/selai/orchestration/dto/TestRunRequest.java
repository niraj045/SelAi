package com.ns.selai.orchestration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Request to start a test run
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestRunRequest {
    @NotNull(message = "Project ID cannot be null")
    private Long projectId;
    @NotBlank(message = "URL cannot be empty")
    private String url;
    @Pattern(regexp = "chrome|firefox|edge", message = "Browser must be chrome, firefox, or edge")
    private String browser; // chrome, firefox, edge
    @Pattern(regexp = "smoke|regression|functional", message = "Test type must be smoke, regression, or functional")
    private String testType; // smoke, regression, functional
    private String userId; // For tracking who initiated the test
}
