package com.ns.selai.reporting.controller;

import com.ns.selai.reporting.dto.ReportGenerateRequest;
import com.ns.selai.reporting.dto.ReportResponse;
import com.ns.selai.reporting.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/generate")
    public ResponseEntity<ReportResponse> generateReport(@Valid @RequestBody ReportGenerateRequest request) {
        log.info("REST request to generate report for test run {}", request.getTestRunId());
        return ResponseEntity.ok(reportService.generateReport(request));
    }

    @GetMapping
    public ResponseEntity<List<ReportResponse>> getReports() {
        return ResponseEntity.ok(reportService.getReports());
    }

    @GetMapping("/{testRunId}")
    public ResponseEntity<ReportResponse> getReport(@PathVariable Long testRunId) {
        return ResponseEntity.ok(reportService.getReport(testRunId));
    }

    @GetMapping("/{testRunId}/execution")
    public ResponseEntity<ReportResponse> getExecutionReport(@PathVariable Long testRunId) {
        return ResponseEntity.ok(reportService.getReport(testRunId));
    }

    @GetMapping(value = "/{testRunId}/html", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getHtmlReport(@PathVariable Long testRunId) {
        return ResponseEntity.ok(reportService.renderHtml(testRunId));
    }

    @GetMapping("/{testRunId}/markdown")
    public ResponseEntity<String> getMarkdownReport(@PathVariable Long testRunId) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/markdown"))
                .body(reportService.renderMarkdown(testRunId));
    }

    @GetMapping("/{testRunId}/pdf")
    public ResponseEntity<byte[]> getPdfReport(@PathVariable Long testRunId) {
        byte[] pdf = reportService.renderPdf(testRunId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("selai-test-run-" + testRunId + ".pdf")
                                .build()
                                .toString())
                .body(pdf);
    }

    @DeleteMapping("/{testRunId}")
    public ResponseEntity<Void> deleteReport(@PathVariable Long testRunId) {
        reportService.deleteReport(testRunId);
        return ResponseEntity.noContent().build();
    }
}
