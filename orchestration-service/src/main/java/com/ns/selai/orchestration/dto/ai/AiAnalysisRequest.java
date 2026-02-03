package com.ns.selai.orchestration.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request sent to Python AI Engine
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiAnalysisRequest {
    private String url;
    private Context context;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Context {
        private String browser; // chrome, firefox, edge
        private String testType; // smoke, regression, functional
    }
}
