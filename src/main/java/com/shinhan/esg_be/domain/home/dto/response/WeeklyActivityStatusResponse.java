package com.shinhan.esg_be.domain.home.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 홈 대시보드 주간 활동 체크 상태를 담는 응답 DTO
 */
@Getter
@AllArgsConstructor
public class WeeklyActivityStatusResponse {

    private final String day;
    private final boolean completed;
}
