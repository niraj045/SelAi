package com.ns.selai.orchestration.client;

import com.ns.selai.orchestration.dto.ai.AiAnalysisRequest;
import com.ns.selai.orchestration.dto.ai.AiAnalysisResponse;
import com.ns.selai.orchestration.dto.ExternalServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;

/**
 * Client to communicate with Python AI Engine
 */
@Service
@Slf4j
public class AiEngineClient {

    private final WebClient webClient;

    @Value("${ai.engine.base-url:http://localhost:5000}")
    private String aiEngineBaseUrl;

    @Value("${ai.engine.fallback-enabled:true}")
    private boolean fallbackEnabled;

    public AiEngineClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public AiAnalysisResponse analyzeAndGenerateTests(String url, String browser, String testType, String userPrompt) {
        log.info("Calling AI Engine to analyze URL: {}", url);

        AiAnalysisRequest request = AiAnalysisRequest.builder()
                .url(url)
                .context(AiAnalysisRequest.Context.builder()
                        .browser(browser)
                        .testType(testType)
                        .build())
                .userPrompt(userPrompt)
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
            if (fallbackEnabled) {
                log.warn("AI Engine unavailable. Using local fallback smoke test for {}: {}", url, e.getMessage());
                return buildFallbackSmokeTest(url);
            }

            log.error("Failed to call AI Engine: ", e);
            throw new ExternalServiceException("AI Engine communication failed: " + e.getMessage(), e);
        }
    }

    public String healSelector(String url, String failedSelector, String errorMessage) {
        log.info("Requesting selector healing for: {}", failedSelector);
        return failedSelector;
    }

    private AiAnalysisResponse buildFallbackSmokeTest(String url) {
        AiAnalysisResponse.TestStep openPage = AiAnalysisResponse.TestStep.builder()
                .action("open_url")
                .url(url)
                .build();

        AiAnalysisResponse.TestStep bodyVisible = AiAnalysisResponse.TestStep.builder()
                .action("assert_element_present")
                .selector("body")
                .build();

        AiAnalysisResponse.TestCase smokeTest = AiAnalysisResponse.TestCase.builder()
                .name("Fallback Smoke Test")
                .description("Opens the target URL and verifies the page body is present.")
                .steps(List.of(openPage, bodyVisible))
                .build();

        return AiAnalysisResponse.builder()
                .tests(List.of(smokeTest))
                .build();
    }
}
