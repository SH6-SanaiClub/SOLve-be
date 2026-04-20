package com.shinhan.esg_be.domain.social.dto.response;

import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Getter
public class DonationDetailResponse {

    private final Long donationId;
    private final String name;
    private final String summary;
    private final String organization;
    private final String description;
    private final Long targetAmount;
    private final Long currentAmount;
    private final String imageUrl;
    private final Long participantCount;
    private final Integer progressPercentage;
    private final Long remainingDays;
    private final LocalDateTime startDate;
    private final LocalDateTime endDate;

    public DonationDetailResponse(
            Long donationId,
            String name,
            String summary,
            String organization,
            String description,
            Long targetAmount,
            Long currentAmount,
            String imageUrl,
            Long participantCount,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        long safeTargetAmount = targetAmount == null ? 0L : targetAmount;
        long safeCurrentAmount = currentAmount == null ? 0L : currentAmount;

        this.donationId = donationId;
        this.name = name;
        this.summary = summary;
        this.organization = organization;
        this.description = description;
        this.targetAmount = targetAmount;
        this.currentAmount = currentAmount;
        this.imageUrl = imageUrl;
        this.participantCount = participantCount;
        this.progressPercentage = safeTargetAmount == 0
                ? 0
                : (int) Math.min(100, Math.round((double) safeCurrentAmount * 100 / safeTargetAmount));
        this.remainingDays = endDate == null
                ? 0L
                : Math.max(0L, ChronoUnit.DAYS.between(LocalDate.now(), endDate.toLocalDate()));
        this.startDate = startDate;
        this.endDate = endDate;
    }
}
