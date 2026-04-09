package com.shinhan.esg_be.domain.home.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 홈 대시보드 등급 진행도 정보를 담는 응답 DTO
 */
@Getter
@AllArgsConstructor
public class GradeProgressResponse {

    private final Integer current;
    private final Integer target;
    private final Integer visualValue;
}
