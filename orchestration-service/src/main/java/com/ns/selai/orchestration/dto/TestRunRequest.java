package com.ns.selai.orchestration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request to start a test run
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestRunRequest {
    private Long projectId;
    private String url;
    private String browser; // chrome, firefox, edge
    private String testType; // smoke, regression, functional
    private String userId; // For tracking who initiated the test
}
