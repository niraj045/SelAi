package com.ns.selai.orchestration.client;

import com.ns.selai.orchestration.dto.ai.AiAnalysisRequest;
import com.ns.selai.orchestration.dto.ai.AiAnalysisResponse;
import com.ns.selai.orchestration.dto.ExternalServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

/**
 * Client to communicate with Python AI Engine
 */
@Service
@Slf4j
public class AiEngineClient {

    private final WebClient webClient;

    @Value("${ai.engine.base-url:http://localhost:5000}")
    private String aiEngineBaseUrl;

    public AiEngineClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public AiAnalysisResponse analyzeAndGenerateTests(String url, String browser, String testType) {
        log.info("Calling AI Engine to analyze URL: {}", url);

        AiAnalysisRequest request = AiAnalysisRequest.builder()
                .url(url)
                .context(AiAnalysisRequest.Context.builder()
                        .browser(browser)
                        .testType(testType)
                        .build())
                .build();

        try {
            AiAnalysisResponse response = webClient.post()
                    .uri(aiEngineBaseUrl + "/api/generate-tests")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(AiAnalysisResponse.class)
                    .timeout(Duration.ofMinutes(5))
                    .block();

            log.info("AI Engine returned {} test cases",
                    response != null && response.getTests() != null ? response.getTests().size() : 0);

            return response;

        } catch (Exception e) {
            log.error("Failed to call AI Engine: ", e);
            throw new ExternalServiceException("AI Engine communication failed: " + e.getMessage(), e);
        }
    }

    public String healSelector(String url, String failedSelector, String errorMessage) {
        log.info("Requesting selector healing for: {}", failedSelector);
        return failedSelector;
    }
}
