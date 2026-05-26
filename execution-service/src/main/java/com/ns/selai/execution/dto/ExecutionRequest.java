package com.ns.selai.execution.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionRequest {

    @NotNull(message = "Test run ID is required")
    private Long testRunId;

    @Pattern(regexp = "chrome|firefox|edge", message = "Browser must be chrome, firefox, or edge")
    private String browser;

    private String callbackUrl;

    @NotEmpty(message = "At least one test case is required")
    @Valid
    private List<TestCase> testCases;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestCase {

        @NotBlank(message = "Test case name is required")
        private String name;

        private String description;

        @NotEmpty(message = "At least one test step is required")
        @Valid
        private List<TestStep> steps;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestStep {

        @NotBlank(message = "Step action is required")
        private String action;

        private String selector;
        private String value;
        private String url;
        private String expectedText;
    }
}
