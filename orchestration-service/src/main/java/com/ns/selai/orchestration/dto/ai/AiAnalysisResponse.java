package com.ns.selai.orchestration.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response from Python AI Engine containing generated test cases
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiAnalysisResponse {
    private List<TestCase> tests;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestCase {
        private String name;
        private String description;
        private List<TestStep> steps;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestStep {
        private String action; // open_url, click, type, submit, assert, etc.
        private String selector; // CSS selector or XPath
        private String value; // For type, assert actions
        private String url; // For open_url action
        private String expectedText; // For assert_text action
    }
}
