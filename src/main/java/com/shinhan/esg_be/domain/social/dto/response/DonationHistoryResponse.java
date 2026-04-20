package com.shinhan.esg_be.domain.social.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class DonationHistoryResponse {

    private final List<DonationHistoryItemResponse> donations;
}
