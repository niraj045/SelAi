package com.ns.selai.orchestration.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enable async processing for test run orchestration
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    // Spring Boot default async executor will be used
}
