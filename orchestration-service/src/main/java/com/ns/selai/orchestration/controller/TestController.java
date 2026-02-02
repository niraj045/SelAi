package com.ns.selai.orchestration.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ns.selai.orchestration.service.TestOrchestrationService;

@RestController
@RequestMapping("/test")
public class TestController {
	
	private TestOrchestrationService testOrchestrationService;
	
	public TestController() {
		this.testOrchestrationService = testOrchestrationService;
	}
	
	@PostMapping("/start")
	public String test(@RequestParam String url) {
		return "Orchestration Service is up and running!";
	}

}
