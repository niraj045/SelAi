package com.ns.selai.orchestration.client;

import com.ns.selai.orchestration.dto.ai.AiAnalysisResponse;
import com.ns.selai.orchestration.dto.ExternalServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;

/**
 * Client to communicate with Execution Service
 */
@Service
@Slf4j
public class ExecutionServiceClient {

    private final WebClient webClient;

    @Value("${execution.service.base-url:http://localhost:8083}")
    private String executionServiceBaseUrl;

    public ExecutionServiceClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public void executeTests(Long testRunId, AiAnalysisResponse aiResponse) {
        log.info("Sending test cases to Execution Service for test run: {}", testRunId);

        try {
            ExecutionRequest request = new ExecutionRequest(testRunId, aiResponse.getTests());

            webClient.post()
                    .uri(executionServiceBaseUrl + "/api/execute")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            log.info("Test execution started for test run: {}", testRunId);

        } catch (Exception e) {
            log.error("Failed to start test execution: ", e);
            throw new ExternalServiceException("Execution Service communication failed: " + e.getMessage(), e);
        }
    }

    private record ExecutionRequest(Long testRunId, List<AiAnalysisResponse.TestCase> testCases) {
    }
}
