package com.shinhan.esg_be.domain.home.dto.response;

import com.shinhan.esg_be.domain.user.entity.enums.Grade;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * 홈 화면에서 사용하는 대시보드 요약 정보를 담는 응답 DTO
 */
@Getter
@AllArgsConstructor
public class HomeDashboardSummaryResponse {

    private final String name;
    private final Grade currentGrade;
    private final Integer totalPoints;
    private final GradeProgressResponse gradeProgress;
    private final List<WeeklyActivityStatusResponse> weeklyActivities;
}
