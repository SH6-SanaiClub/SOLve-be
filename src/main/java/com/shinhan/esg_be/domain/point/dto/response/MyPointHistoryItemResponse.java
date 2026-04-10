package com.shinhan.esg_be.domain.point.dto.response;

import com.shinhan.esg_be.domain.point.entity.enums.PointReason;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class MyPointHistoryItemResponse {

    private final Long userPointId;
    private final String title;
    private final PointReason reason;
    private final Long changedAmount;
    private final Long pointAfter;
    private final LocalDateTime createdAt;
}
