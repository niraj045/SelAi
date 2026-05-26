package com.ns.selai.orchestration.client;

import com.ns.selai.orchestration.model.TestRun;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;

@Service
@Slf4j
public class ReportingServiceClient {

    private final WebClient webClient;

    @Value("${reporting.service.base-url:http://localhost:8084}")
    private String reportingServiceBaseUrl;

    @Value("${reporting.service.enabled:true}")
    private boolean reportingEnabled;

    public ReportingServiceClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public void generateReport(TestRun testRun) {
        if (!reportingEnabled) {
            return;
        }

        try {
            ReportGenerateRequest request = new ReportGenerateRequest(
                    testRun.getId(),
                    "Project " + testRun.getProjectId(),
                    testRun.getUrl(),
                    testRun.getStatus().name(),
                    testRun.getTotalTests(),
                    testRun.getPassedTests(),
                    testRun.getFailedTests(),
                    testRun.getErrorMessage(),
                    List.of());

            webClient.post()
                    .uri(reportingServiceBaseUrl + "/api/reports/generate")
                    .bodyValue(request)
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofSeconds(15))
                    .block();

            log.info("Report generation requested for test run {}", testRun.getId());
        } catch (Exception e) {
            log.warn("Report generation failed for test run {}: {}", testRun.getId(), e.getMessage());
        }
    }

    private record ReportGenerateRequest(
            Long testRunId,
            String projectName,
            String url,
            String status,
            Integer totalTests,
            Integer passedTests,
            Integer failedTests,
            String errorMessage,
            List<Object> testCases) {
    }
}
