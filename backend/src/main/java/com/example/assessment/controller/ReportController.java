package com.example.assessment.controller;

import com.example.assessment.model.UserRole;
import com.example.assessment.service.AuthService;
import com.example.assessment.service.ReportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final ReportService reportService;
    private final AuthService authService;

    public ReportController(ReportService reportService, AuthService authService) {
        this.reportService = reportService;
        this.authService = authService;
    }

    @GetMapping({"/score-analysis.xls", "/score-analysis.xlsx"})
    public ResponseEntity<byte[]> scoreAnalysis(@RequestHeader(value = "X-Username", required = false) String username,
                                                @RequestHeader(value = "X-User-Role", required = false) String role) throws IOException {
        authService.requireAnyRole(username, role, UserRole.ADMIN, UserRole.TEACHER);
        return download(
                reportService.buildScoreAnalysisExcel(),
                "score-analysis.xls",
                "application/vnd.ms-excel"
        );
    }

    @GetMapping("/objective-report.docx")
    public ResponseEntity<byte[]> objectiveReport(@RequestHeader(value = "X-Username", required = false) String username,
                                                  @RequestHeader(value = "X-User-Role", required = false) String role) throws IOException {
        authService.requireAnyRole(username, role, UserRole.ADMIN, UserRole.TEACHER);
        return download(
                reportService.buildObjectiveReportDocx(),
                "objective-report.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        );
    }

    private ResponseEntity<byte[]> download(byte[] content, String fileName, String contentType) {
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(contentType))
                .body(content);
    }
}
