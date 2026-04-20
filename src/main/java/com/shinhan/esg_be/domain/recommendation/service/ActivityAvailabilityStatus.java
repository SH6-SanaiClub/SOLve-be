package com.shinhan.esg_be.domain.recommendation.service;

public record ActivityAvailabilityStatus(
        boolean canParticipate,
        boolean alreadyParticipatedToday,
        boolean monthlyLimitReached,
        String blockedReasonCode
) {
}
