package com.shinhan.esg_be.domain.report.controller;

import com.shinhan.esg_be.domain.report.dto.response.ReportPreviewResponse;
import com.shinhan.esg_be.domain.report.dto.response.ReportPdfFile;
import com.shinhan.esg_be.domain.report.dto.request.ReportIssueRequest;
import com.shinhan.esg_be.domain.report.dto.response.ReportIssueResponse;
import com.shinhan.esg_be.domain.report.service.ReportIssueService;
import com.shinhan.esg_be.domain.report.service.MyReportService;
import com.shinhan.esg_be.domain.report.service.ReportPdfService;
import com.shinhan.esg_be.global.security.AuthContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/my/reports")
public class MyReportController {

    private final MyReportService myReportService;
    private final ReportIssueService reportIssueService;
    private final ReportPdfService reportPdfService;
    private final AuthContext authContext;

    @GetMapping("/preview")
    public ResponseEntity<ReportPreviewResponse> getPreview(
            @RequestParam(defaultValue = "3M") String periodType
    ) {
        return ResponseEntity.ok(
                myReportService.getPreview(authContext.currentUserId(), periodType)
        );
    }

    @PostMapping("/issues")
    public ResponseEntity<ReportIssueResponse> issue(
            @Valid @RequestBody ReportIssueRequest request
    ) {
        return ResponseEntity.ok(
                reportIssueService.issue(authContext.currentUserId(), request)
        );
    }

    @GetMapping("/issues/{issueId}/pdf")
    public ResponseEntity<byte[]> downloadPdf(
            @PathVariable Long issueId
    ) {
        ReportPdfFile pdfFile = reportPdfService.download(authContext.currentUserId(), issueId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + pdfFile.fileName() + "\"")
                .body(pdfFile.content());
    }
}
