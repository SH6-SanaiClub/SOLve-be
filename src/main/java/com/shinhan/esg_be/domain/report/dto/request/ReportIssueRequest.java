package com.shinhan.esg_be.domain.report.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ReportIssueRequest(
        @NotBlank
        @Pattern(regexp = "^(3M|6M|1Y)$", message = "periodType must be one of 3M, 6M, 1Y.")
        String periodType
) {
}
