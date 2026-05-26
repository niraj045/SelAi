package com.ns.selai.api_gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Configuration
public class GatewayConfig {

    private static final String CORS_RESPONSE_HEADERS =
            "Access-Control-Allow-Origin Access-Control-Allow-Credentials";
    private static final String DEDUPE_STRATEGY = "RETAIN_FIRST";

    @Bean
    public RouteLocator selAiRoutes(
            RouteLocatorBuilder builder,
            @Value("${services.test-management.url:http://localhost:8081}") String testManagementUrl,
            @Value("${services.orchestration.url:http://localhost:8082}") String orchestrationUrl,
            @Value("${services.execution.url:http://localhost:8083}") String executionUrl,
            @Value("${services.reporting.url:http://localhost:8084}") String reportingUrl) {

        return builder.routes()
                .route("test-management-service", route -> route
                        .path("/api/projects/**", "/api/test-suites/**")
                        .filters(f -> f.dedupeResponseHeader(CORS_RESPONSE_HEADERS, DEDUPE_STRATEGY))
                        .uri(testManagementUrl))
                .route("orchestration-service", route -> route
                        .path("/api/test-runs/**", "/api/tests/**")
                        .filters(f -> f.dedupeResponseHeader(CORS_RESPONSE_HEADERS, DEDUPE_STRATEGY))
                        .uri(orchestrationUrl))
                .route("execution-service", route -> route
                        .path("/api/execute/**")
                        .filters(f -> f.dedupeResponseHeader(CORS_RESPONSE_HEADERS, DEDUPE_STRATEGY))
                        .uri(executionUrl))
                .route("reporting-service", route -> route
                        .path("/api/reports/**")
                        .filters(f -> f.dedupeResponseHeader(CORS_RESPONSE_HEADERS, DEDUPE_STRATEGY))
                        .uri(reportingUrl))
                .build();
    }

    /**
     * Handles CORS preflight (OPTIONS) requests directly at the gateway —
     * they never reach downstream services, so no 503 when a service is down.
     */
    @Bean
    public WebFilter corsPreflightFilter() {
        return (ServerWebExchange exchange, WebFilterChain chain) -> {
            if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
                exchange.getResponse().setStatusCode(HttpStatus.OK);
                HttpHeaders headers = exchange.getResponse().getHeaders();
                String origin = exchange.getRequest().getHeaders().getOrigin();
                headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin != null ? origin : "*");
                headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                        "GET,POST,PUT,PATCH,DELETE,OPTIONS");
                headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                        "Content-Type,Authorization,X-Requested-With,Accept,Origin");
                headers.set(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600");
                headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "false");
                return exchange.getResponse().setComplete();
            }
            return chain.filter(exchange);
        };
    }

    @Bean
    public CorsWebFilter corsWebFilter(
            @Value("${gateway.cors.allowed-origins:*}") String allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(split(allowedOrigins));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of(HttpHeaders.CONTENT_DISPOSITION, "Content-Type"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }

    private List<String> split(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
