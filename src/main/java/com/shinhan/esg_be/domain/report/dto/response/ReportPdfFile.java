package com.shinhan.esg_be.domain.report.dto.response;

public record ReportPdfFile(
        String fileName,
        byte[] content
) {
}
