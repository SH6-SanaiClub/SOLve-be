package com.shinhan.esg_be.domain.social.dto.response;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class DonationHistoryItemResponse {

    private final Long donationLogId;
    private final Long donationId;
    private final String organization;
    private final String name;
    private final String imageUrl;
    private final LocalDateTime donatedAt;
    private final Long amount;

    public DonationHistoryItemResponse(
            Long donationLogId,
            Long donationId,
            String organization,
            String name,
            String imageUrl,
            LocalDateTime donatedAt,
            Long amount
    ) {
        this.donationLogId = donationLogId;
        this.donationId = donationId;
        this.organization = organization;
        this.name = name;
        this.imageUrl = imageUrl;
        this.donatedAt = donatedAt;
        this.amount = amount;
    }
}
