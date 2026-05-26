package com.ns.selai.reporting.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ns.selai.reporting.dto.ReportGenerateRequest;
import com.ns.selai.reporting.dto.ReportGenerateRequest.TestCaseResult;
import com.ns.selai.reporting.dto.ReportResponse;
import com.ns.selai.reporting.model.TestReport;
import com.ns.selai.reporting.repository.TestReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final TestReportRepository testReportRepository;
    private final ObjectMapper objectMapper;
    private final PdfReportRenderer pdfReportRenderer;

    @Transactional
    public ReportResponse generateReport(ReportGenerateRequest request) {
        TestReport report = testReportRepository.findByTestRunId(request.getTestRunId())
                .orElseGet(() -> TestReport.builder()
                        .testRunId(request.getTestRunId())
                        .build());

        report.setProjectName(defaultString(request.getProjectName(), "Project " + request.getTestRunId()));
        report.setUrl(request.getUrl());
        report.setStatus(defaultString(request.getStatus(), "UNKNOWN"));
        report.setTotalTests(defaultInt(request.getTotalTests()));
        report.setPassedTests(defaultInt(request.getPassedTests()));
        report.setFailedTests(defaultInt(request.getFailedTests()));
        report.setErrorMessage(request.getErrorMessage());
        report.setTestCasesJson(writeTestCases(request.getTestCases()));

        TestReport savedReport = testReportRepository.save(report);
        log.info("Generated report {} for test run {}", savedReport.getId(), savedReport.getTestRunId());
        return toResponse(savedReport);
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> getReports() {
        return testReportRepository.findAllByOrderByGeneratedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReportResponse getReport(Long testRunId) {
        return toResponse(findReport(testRunId));
    }

    @Transactional(readOnly = true)
    public String renderHtml(Long testRunId) {
        TestReport report = findReport(testRunId);
        List<TestCaseResult> testCases = readTestCases(report);

        StringBuilder html = new StringBuilder();
        html.append("<!doctype html><html><head><meta charset=\"utf-8\"><title>SelAi Test Report</title>");
        html.append("<style>");
        html.append("body{font-family:Arial,sans-serif;margin:32px;color:#17202a}");
        html.append("table{border-collapse:collapse;width:100%;margin-top:20px}");
        html.append("th,td{border:1px solid #d5d8dc;padding:10px;text-align:left}");
        html.append("th{background:#f4f6f7}.passed{color:#137333}.failed{color:#b3261e}");
        html.append("</style></head><body>");
        html.append("<h1>SelAi Test Report</h1>");
        html.append("<p><strong>Project:</strong> ").append(escapeHtml(report.getProjectName())).append("</p>");
        html.append("<p><strong>URL:</strong> ").append(escapeHtml(report.getUrl())).append("</p>");
        html.append("<p><strong>Status:</strong> ").append(escapeHtml(report.getStatus())).append("</p>");
        html.append("<p><strong>Total:</strong> ").append(defaultInt(report.getTotalTests()));
        html.append(" | <strong>Passed:</strong> ").append(defaultInt(report.getPassedTests()));
        html.append(" | <strong>Failed:</strong> ").append(defaultInt(report.getFailedTests()));
        html.append(" | <strong>Pass rate:</strong> ").append(String.format("%.2f%%", passRate(report))).append("</p>");

        if (StringUtils.hasText(report.getErrorMessage())) {
            html.append("<p><strong>Error:</strong> ").append(escapeHtml(report.getErrorMessage())).append("</p>");
        }

        html.append("<table><thead><tr><th>Test</th><th>Status</th><th>Time</th><th>Error</th><th>Screenshot</th></tr></thead><tbody>");
        for (TestCaseResult testCase : testCases) {
            String statusClass = "PASSED".equalsIgnoreCase(testCase.getStatus()) ? "passed" : "failed";
            html.append("<tr><td>").append(escapeHtml(testCase.getName())).append("</td>");
            html.append("<td class=\"").append(statusClass).append("\">").append(escapeHtml(testCase.getStatus())).append("</td>");
            html.append("<td>").append(testCase.getExecutionTimeMs() == null ? "" : testCase.getExecutionTimeMs() + " ms").append("</td>");
            html.append("<td>").append(escapeHtml(testCase.getErrorMessage())).append("</td>");
            html.append("<td>").append(escapeHtml(testCase.getScreenshotPath())).append("</td></tr>");
        }
        html.append("</tbody></table></body></html>");
        return html.toString();
    }

    @Transactional(readOnly = true)
    public String renderMarkdown(Long testRunId) {
        TestReport report = findReport(testRunId);
        List<TestCaseResult> testCases = readTestCases(report);

        StringBuilder markdown = new StringBuilder();
        markdown.append("# SelAi Test Report\n\n");
        markdown.append("- Project: ").append(defaultString(report.getProjectName(), "N/A")).append("\n");
        markdown.append("- URL: ").append(defaultString(report.getUrl(), "N/A")).append("\n");
        markdown.append("- Status: ").append(defaultString(report.getStatus(), "UNKNOWN")).append("\n");
        markdown.append("- Total: ").append(defaultInt(report.getTotalTests())).append("\n");
        markdown.append("- Passed: ").append(defaultInt(report.getPassedTests())).append("\n");
        markdown.append("- Failed: ").append(defaultInt(report.getFailedTests())).append("\n");
        markdown.append("- Pass rate: ").append(String.format("%.2f%%", passRate(report))).append("\n\n");

        if (StringUtils.hasText(report.getErrorMessage())) {
            markdown.append("## Error\n\n").append(report.getErrorMessage()).append("\n\n");
        }

        markdown.append("## Test Cases\n\n");
        markdown.append("| Test | Status | Time | Error | Screenshot |\n");
        markdown.append("| --- | --- | ---: | --- | --- |\n");
        for (TestCaseResult testCase : testCases) {
            markdown.append("| ")
                    .append(markdownCell(testCase.getName()))
                    .append(" | ")
                    .append(markdownCell(testCase.getStatus()))
                    .append(" | ")
                    .append(testCase.getExecutionTimeMs() == null ? "" : testCase.getExecutionTimeMs() + " ms")
                    .append(" | ")
                    .append(markdownCell(testCase.getErrorMessage()))
                    .append(" | ")
                    .append(markdownCell(testCase.getScreenshotPath()))
                    .append(" |\n");
        }
        return markdown.toString();
    }

    @Transactional(readOnly = true)
    public byte[] renderPdf(Long testRunId) {
        TestReport report = findReport(testRunId);
        List<String> lines = new ArrayList<>();
        lines.add("SelAi Test Report");
        lines.add("Project: " + defaultString(report.getProjectName(), "N/A"));
        lines.add("URL: " + defaultString(report.getUrl(), "N/A"));
        lines.add("Status: " + defaultString(report.getStatus(), "UNKNOWN"));
        lines.add("Total: " + defaultInt(report.getTotalTests()));
        lines.add("Passed: " + defaultInt(report.getPassedTests()));
        lines.add("Failed: " + defaultInt(report.getFailedTests()));
        lines.add("Pass rate: " + String.format("%.2f%%", passRate(report)));

        if (StringUtils.hasText(report.getErrorMessage())) {
            lines.add("Error: " + report.getErrorMessage());
        }

        for (TestCaseResult testCase : readTestCases(report)) {
            lines.add("- " + defaultString(testCase.getName(), "Unnamed test")
                    + " | " + defaultString(testCase.getStatus(), "UNKNOWN")
                    + " | " + (testCase.getExecutionTimeMs() == null ? "N/A" : testCase.getExecutionTimeMs() + " ms"));
        }
        return pdfReportRenderer.render(lines);
    }

    @Transactional
    public void deleteReport(Long testRunId) {
        TestReport report = findReport(testRunId);
        testReportRepository.delete(report);
    }

    private TestReport findReport(Long testRunId) {
        return testReportRepository.findByTestRunId(testRunId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found for test run ID: " + testRunId));
    }

    private String writeTestCases(List<TestCaseResult> testCases) {
        try {
            return objectMapper.writeValueAsString(testCases == null ? List.of() : testCases);
        } catch (JsonProcessingException e) {
            log.warn("Unable to serialize test case results: {}", e.getMessage());
            return "[]";
        }
    }

    private List<TestCaseResult> readTestCases(TestReport report) {
        try {
            if (!StringUtils.hasText(report.getTestCasesJson())) {
                return List.of();
            }
            return objectMapper.readValue(report.getTestCasesJson(), new TypeReference<>() {
            });
        } catch (Exception e) {
            log.warn("Unable to read test cases for report {}: {}", report.getId(), e.getMessage());
            return List.of();
        }
    }

    private ReportResponse toResponse(TestReport report) {
        return ReportResponse.builder()
                .id(report.getId())
                .testRunId(report.getTestRunId())
                .projectName(report.getProjectName())
                .url(report.getUrl())
                .status(report.getStatus())
                .totalTests(defaultInt(report.getTotalTests()))
                .passedTests(defaultInt(report.getPassedTests()))
                .failedTests(defaultInt(report.getFailedTests()))
                .passRate(passRate(report))
                .errorMessage(report.getErrorMessage())
                .generatedAt(report.getGeneratedAt())
                .updatedAt(report.getUpdatedAt())
                .htmlUrl("/api/reports/" + report.getTestRunId() + "/html")
                .markdownUrl("/api/reports/" + report.getTestRunId() + "/markdown")
                .pdfUrl("/api/reports/" + report.getTestRunId() + "/pdf")
                .build();
    }

    private double passRate(TestReport report) {
        int total = defaultInt(report.getTotalTests());
        if (total == 0) {
            return 0.0;
        }
        return (defaultInt(report.getPassedTests()) * 100.0) / total;
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String defaultString(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String markdownCell(String value) {
        return value == null ? "" : value.replace("|", "\\|").replace("\n", " ");
    }
}
