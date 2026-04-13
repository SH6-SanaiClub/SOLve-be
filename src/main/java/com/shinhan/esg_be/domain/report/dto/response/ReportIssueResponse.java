package com.shinhan.esg_be.domain.report.dto.response;

public record ReportIssueResponse(
        ReportCertificateMetaResponse certificate,
        ReportCertificateSnapshotResponse snapshot
) {
}
