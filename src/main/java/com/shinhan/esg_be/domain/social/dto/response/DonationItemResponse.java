package com.shinhan.esg_be.domain.social.dto.response;

import com.shinhan.esg_be.domain.social.repository.DonationListProjection;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
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

    public static DonationItemResponse from(DonationListProjection projection) {
        long targetAmount = projection.getTargetAmount() == null ? 0L : projection.getTargetAmount();
        long currentAmount = projection.getCurrentAmount() == null ? 0L : projection.getCurrentAmount();
        int progressPercentage = targetAmount == 0
                ? 0
                : (int) Math.min(100, Math.round((double) currentAmount * 100 / targetAmount));

        return new DonationItemResponse(
                projection.getDonationId(),
                projection.getName(),
                projection.getDescription(),
                projection.getTargetAmount(),
                projection.getCurrentAmount(),
                projection.getImageUrl(),
                projection.getParticipantCount(),
                progressPercentage,
                projection.getStartDate(),
                projection.getEndDate()
        );
    }
}
