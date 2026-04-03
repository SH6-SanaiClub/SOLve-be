package com.shinhan.esg_be.domain.social.dto.response;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class DonationItemResponse {

    private final Long donationId;
    private final String name;
    private final String description;
    private final Long targetAmount;
    private final Long currentAmount;
    private final String imageUrl;
    private final Long participantCount;
    private final Integer progressPercentage;
    private final LocalDateTime startDate;
    private final LocalDateTime endDate;

    public DonationItemResponse(
            Long donationId,
            String name,
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
        int progressPercentage = safeTargetAmount == 0
                ? 0
                : (int) Math.min(100, Math.round((double) safeCurrentAmount * 100 / safeTargetAmount));

        this.donationId = donationId;
        this.name = name;
        this.description = description;
        this.targetAmount = targetAmount;
        this.currentAmount = currentAmount;
        this.imageUrl = imageUrl;
        this.participantCount = participantCount;
        this.progressPercentage = progressPercentage;
        this.startDate = startDate;
        this.endDate = endDate;
    }
}
