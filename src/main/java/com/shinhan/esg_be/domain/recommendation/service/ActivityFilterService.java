package com.shinhan.esg_be.domain.recommendation.service;

import com.shinhan.esg_be.domain.activity.repository.UserActivityRepository;
import com.shinhan.esg_be.domain.quiz.repository.UserQuizRepository;
import com.shinhan.esg_be.domain.recommendation.dto.ActivityCandidateDto;
import com.shinhan.esg_be.domain.recommendation.dto.UserFeatureDto;
import com.shinhan.esg_be.domain.social.repository.UserDonationRepository;
import com.shinhan.esg_be.domain.social.repository.UserEcoProductRepository;
import com.shinhan.esg_be.domain.volunteer.entity.enums.VolunteerStatus;
import com.shinhan.esg_be.domain.volunteer.repository.UserVolunteerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityFilterService {

    private final UserActivityRepository userActivityRepository;
    private final UserDonationRepository userDonationRepository;
    private final UserVolunteerRepository userVolunteerRepository;
    private final UserEcoProductRepository userEcoProductRepository;
    private final UserQuizRepository userQuizRepository;

    private static final int MONTHLY_E_MAX = 5;
    private static final int MONTHLY_S_MAX = 25;
    private static final int MONTHLY_G_MAX = 10;

    public List<ActivityCandidateDto> filter(
            List<ActivityCandidateDto> candidates,
            UserFeatureDto feature
    ) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        return candidates.stream()
                .filter(c -> passesAllFilters(c, feature, startOfDay))
                .collect(Collectors.toList());
    }

    private boolean passesAllFilters(
            ActivityCandidateDto c,
            UserFeatureDto feature,
            LocalDateTime startOfDay
    ) {
        // 필터 1: 월 카테고리 한도 도달 여부
        if (isMonthlyLimitReached(c, feature)) {
            log.debug("월 한도 필터 탈락 - activityType={} referenceId={}", c.getActivityType(), c.getReferenceId());
            return false;
        }

        // 필터 2~6: 활동 유형별 수행 가능 여부
        return switch (c.getActivityType()) {
            case "PHOTO" -> passesPhotoFilter(c, feature.getUserId(), startOfDay);
            case "DONATION" -> passesDonationFilter(c, feature.getUserId(), startOfDay);
            case "VOLUNTEER" -> passesVolunteerFilter(c, feature.getUserId());
            case "PURCHASE" -> passesPurchaseFilter(c);
            case "QUIZ" -> passesQuizFilter(feature.getUserId(), startOfDay);
            default -> false;
        };
    }

    // 필터 1: 월 카테고리 한도 도달
    private boolean isMonthlyLimitReached(ActivityCandidateDto c, UserFeatureDto feature) {
        return switch (c.getScoreCategory()) {
            case "E" -> feature.getMonthlyEScore() >= MONTHLY_E_MAX;
            case "S" -> feature.getMonthlySScore() >= MONTHLY_S_MAX;
            case "G" -> feature.getMonthlyGScore() >= MONTHLY_G_MAX;
            default -> false;
        };
    }

    // 필터 2: E 사진인증 — 오늘 해당 activity 이미 승인 완료
    private boolean passesPhotoFilter(ActivityCandidateDto c, Long userId, LocalDateTime startOfDay) {
        long count = userActivityRepository.countTodayApproved(userId, c.getReferenceId(), startOfDay);
        return count == 0;
    }

    // 필터 3: 기부 — 캠페인 기간 외 또는 비활성
    private boolean passesDonationFilter(ActivityCandidateDto c, Long userId, LocalDateTime startOfDay) {
        if (!c.isActive()) {
            return false;
        }
        if (c.getDeadlineDate() != null && c.getDeadlineDate().isBefore(LocalDate.now())) {
            return false;
        }
        long todayCount = userDonationRepository.countTodayByDonation(userId, c.getReferenceId(), startOfDay);
        return todayCount == 0;
    }

    // 필터 4: 봉사 — 마감 또는 정원 초과 (isActive, deadlineDate는 CandidateLoader에서 이미 걸렀으나 재확인)
    private boolean passesVolunteerFilter(ActivityCandidateDto c, Long userId) {
        if (!c.isActive()) {
            return false;
        }
        if (c.getDeadlineDate() != null && !c.getDeadlineDate().isAfter(LocalDate.now())) {
            return false;
        }
        return !userVolunteerRepository.existsByUser_UserIdAndVolunteer_VolunteerIdAndStatusNot(
                userId, c.getReferenceId(), VolunteerStatus.NOSHOW
        );
    }

    // 필터 5: 상품구매 — 재고 없음 또는 비활성 (isActive는 CandidateLoader에서 이미 처리)
    private boolean passesPurchaseFilter(ActivityCandidateDto c) {
        if (!c.isActive()) {
            return false;
        }
        return c.getDeadlineDate() == null || !c.getDeadlineDate().isBefore(LocalDate.now());
    }

    // 필터 6: 퀴즈 — 오늘 이미 참여
    private boolean passesQuizFilter(Long userId, LocalDateTime startOfDay) {
        return userQuizRepository.countToday(userId, startOfDay) == 0;
    }
}
