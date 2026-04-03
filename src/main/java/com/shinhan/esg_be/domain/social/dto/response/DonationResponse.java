package com.shinhan.esg_be.domain.social.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class DonationResponse {

    private final DonationSummaryResponse summary;
    private final List<DonationItemResponse> donations;
}
