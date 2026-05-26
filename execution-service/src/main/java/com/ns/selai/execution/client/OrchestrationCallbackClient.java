package com.ns.selai.execution.client;

import com.ns.selai.execution.dto.ExecutionRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrchestrationCallbackClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${orchestration.service.url:http://localhost:8082}")
    private String orchestrationServiceUrl;

    public void publishResults(ExecutionRequest request, ExecutionSummary summary) {
        String callbackUrl = StringUtils.hasText(request.getCallbackUrl())
                ? request.getCallbackUrl()
                : orchestrationServiceUrl + "/api/test-runs/" + request.getTestRunId() + "/results";

        try {
            webClientBuilder.build()
                    .post()
                    .uri(callbackUrl)
                    .bodyValue(summary)
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofSeconds(15))
                    .block();

            log.info("Published execution results for test run {} to orchestration", request.getTestRunId());
        } catch (Exception e) {
            log.warn("Unable to publish execution results for test run {}: {}",
                    request.getTestRunId(), e.getMessage());
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecutionSummary {
        private Long testRunId;
        private Integer total;
        private Integer passed;
        private Integer failed;
        private String status;
        private String errorMessage;
    }
}
