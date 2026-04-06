package com.shinhan.esg_be.domain.social.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DonationSummaryResponse {

    private final Long totalDonationAmount;
    private final Long totalParticipantCount;
    private final Integer donationCount;
}
