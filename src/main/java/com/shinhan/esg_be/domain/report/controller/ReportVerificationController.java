package com.shinhan.esg_be.domain.report.controller;

import com.shinhan.esg_be.domain.report.dto.response.ReportVerificationResponse;
import com.shinhan.esg_be.domain.report.service.ReportVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reports")
public class ReportVerificationController {

    private final ReportVerificationService reportVerificationService;

    @GetMapping("/verify/{token}")
    public ResponseEntity<ReportVerificationResponse> verify(
            @PathVariable String token
    ) {
        return ResponseEntity.ok(reportVerificationService.verify(token));
    }
}
