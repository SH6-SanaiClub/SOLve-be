package com.shinhan.esg_be.domain.recommendation.service;

import com.shinhan.esg_be.domain.quiz.repository.UserQuizRepository;
import com.shinhan.esg_be.domain.recommendation.dto.ActivityCandidateDto;
import com.shinhan.esg_be.domain.recommendation.dto.UserFeatureDto;
import com.shinhan.esg_be.domain.social.repository.UserDonationRepository;
import com.shinhan.esg_be.domain.volunteer.repository.UserVolunteerRepository;
import com.shinhan.esg_be.domain.environment.repository.UserEnvironmentActivityRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityAvailabilityServiceTest {

    @Mock
    private UserEnvironmentActivityRepository userEnvironmentActivityRepository;

    @Mock
    private UserDonationRepository userDonationRepository;

    @Mock
    private UserVolunteerRepository userVolunteerRepository;

    @Mock
    private UserQuizRepository userQuizRepository;

    @Mock
    private RecommendationPolicyService recommendationPolicyService;

    @InjectMocks
    private ActivityAvailabilityService activityAvailabilityService;

    @Test
    @DisplayName("E 활동은 오늘 승인 여부가 아니라 시도 여부로 막는다")
    void evaluate_photoBlockedByTodayAttempt() {
        ActivityCandidateDto candidate = ActivityCandidateDto.builder()
                .activityType("PHOTO")
                .referenceId(10L)
                .scoreCategory("E")
                .isActive(true)
                .build();
        UserFeatureDto feature = UserFeatureDto.builder()
                .userId(1L)
                .monthlyEScore(0)
                .build();

        when(recommendationPolicyService.getMonthlyMaxScore("E")).thenReturn(5);
        when(userEnvironmentActivityRepository.countTodayAttempts(anyLong(), anyLong(), any())).thenReturn(1L);

        ActivityAvailabilityStatus status = activityAvailabilityService.evaluate(candidate, feature);

        assertThat(status.canParticipate()).isFalse();
        assertThat(status.alreadyParticipatedToday()).isTrue();
        assertThat(status.monthlyLimitReached()).isFalse();
        assertThat(status.blockedReasonCode())
                .isEqualTo(ActivityAvailabilityService.BLOCKED_PHOTO_ALREADY_ATTEMPTED_TODAY);
    }

    @Test
    @DisplayName("봉사활동은 신청 이력이 있으면 추천에서 막는다")
    void evaluate_volunteerBlockedByApplication() {
        ActivityCandidateDto candidate = ActivityCandidateDto.builder()
                .activityType("VOLUNTEER")
                .referenceId(20L)
                .scoreCategory("S")
                .isActive(true)
                .deadlineDate(LocalDate.now().plusDays(3))
                .build();
        UserFeatureDto feature = UserFeatureDto.builder()
                .userId(2L)
                .monthlySScore(0)
                .build();

        when(recommendationPolicyService.getMonthlyMaxScore("S")).thenReturn(25);
        when(userVolunteerRepository.existsByUser_UserIdAndVolunteer_VolunteerId(2L, 20L)).thenReturn(true);

        ActivityAvailabilityStatus status = activityAvailabilityService.evaluate(candidate, feature);

        assertThat(status.canParticipate()).isFalse();
        assertThat(status.blockedReasonCode())
                .isEqualTo(ActivityAvailabilityService.BLOCKED_VOLUNTEER_ALREADY_APPLIED);
    }

    @Test
    @DisplayName("월 한도는 정책 테이블 기준으로 막는다")
    void evaluate_blockedByMonthlyLimitFromPolicy() {
        ActivityCandidateDto candidate = ActivityCandidateDto.builder()
                .activityType("QUIZ")
                .referenceId(30L)
                .scoreCategory("G")
                .isActive(true)
                .build();
        UserFeatureDto feature = UserFeatureDto.builder()
                .userId(3L)
                .monthlyGScore(12)
                .build();

        when(recommendationPolicyService.getMonthlyMaxScore("G")).thenReturn(10);

        ActivityAvailabilityStatus status = activityAvailabilityService.evaluate(candidate, feature);

        assertThat(status.canParticipate()).isFalse();
        assertThat(status.monthlyLimitReached()).isTrue();
        assertThat(status.blockedReasonCode())
                .isEqualTo(ActivityAvailabilityService.BLOCKED_MONTHLY_LIMIT_REACHED);
    }
}
