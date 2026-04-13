package com.shinhan.esg_be.domain.point.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MyPointSummaryResponse {

    private final Long userId;
    private final Long totalPoints;
}
