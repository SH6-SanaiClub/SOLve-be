package com.shinhan.esg_be.domain.bank.service.screening;

public record VirtualCbAssessmentResult(
        int cbScore,
        boolean hasTelecomDelinquency
) {
}
