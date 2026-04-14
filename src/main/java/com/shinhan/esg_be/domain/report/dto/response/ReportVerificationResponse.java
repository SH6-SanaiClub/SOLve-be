package com.shinhan.esg_be.domain.report.dto.response;

public record ReportVerificationResponse(
        Boolean valid,
        String verificationStatus,
        ReportCertificateMetaResponse certificate,
        ReportCertificateSnapshotResponse snapshot
) {
}
