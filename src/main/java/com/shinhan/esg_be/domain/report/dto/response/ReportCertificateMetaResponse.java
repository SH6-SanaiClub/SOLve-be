package com.shinhan.esg_be.domain.report.dto.response;

import java.time.LocalDateTime;

public record ReportCertificateMetaResponse(
        Long issueId,
        String certificateNumber,
        String verificationCode,
        String verificationUrl,
        LocalDateTime issuedAt
) {
}
