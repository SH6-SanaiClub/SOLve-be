package com.shinhan.esg_be.domain.point.service;

import com.shinhan.esg_be.domain.point.entity.UserPoint;
import com.shinhan.esg_be.domain.point.entity.enums.PointCategory;
import com.shinhan.esg_be.domain.point.repository.UserPointRepository;
import com.shinhan.esg_be.domain.point.service.command.ApplyActivityPointCommand;
import com.shinhan.esg_be.domain.point.service.command.PointCalculationCommand;
import com.shinhan.esg_be.domain.point.service.result.ApplyActivityPointResult;
import com.shinhan.esg_be.domain.point.service.result.PointCalculationResult;
import com.shinhan.esg_be.domain.policy.entity.ActivityRewardPolicy;
import com.shinhan.esg_be.domain.policy.entity.PointPolicy;
import com.shinhan.esg_be.domain.policy.entity.enums.PointPolicyType;
import com.shinhan.esg_be.domain.policy.repository.ActivityRewardPolicyRepository;
import com.shinhan.esg_be.domain.policy.repository.PointPolicyRepository;
import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.repository.UserRepository;
import com.shinhan.esg_be.global.common.enums.ActivityType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointService {

    private static final Set<PointCategory> QUIZ_CATEGORIES = EnumSet.of(PointCategory.QUIZ_CORRECT, PointCategory.QUIZ_WRONG);

    private final UserRepository userRepository;
    private final ActivityRewardPolicyRepository activityRewardPolicyRepository;
    private final PointPolicyRepository pointPolicyRepository;
    private final UserPointRepository userPointRepository;
    private final PointCalculatorService pointCalculatorService;
    private final Clock clock;

    @Transactional
    public ApplyActivityPointResult applyActivityPoint(ApplyActivityPointCommand command) {
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        ActivityType activityType = command.activityType();
        LocalDateTime activityDateTime = resolveActivityDateTime(command.activityDateTime());
        PointCategory pointCategory = mapToPointCategory(activityType);

        ActivityRewardPolicy activityRewardPolicy = activityRewardPolicyRepository.findByActivityType(activityType)
                .orElseThrow(() -> new IllegalArgumentException("Activity reward policy not found."));

        int activityPoint = calculateActivityPoint(user, pointCategory, activityRewardPolicy, command.amount());
        int pointAfter = user.getTotalPoints();

        if (activityPoint > 0) {
            pointAfter = savePointHistory(user, pointCategory, activityPoint);
        }

        int bonusPoint = calculateAndApplyBonus(user, pointCategory, activityDateTime);

        return new ApplyActivityPointResult(
                pointCategory,
                activityPoint,
                bonusPoint,
                pointAfter + bonusPoint,
                false
        );
    }

    @Transactional
    public int reclaimAbusedPoint(Long userId, int pointAmount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        if (pointAmount <= 0) {
            return user.getTotalPoints();
        }

        return savePointHistory(user, PointCategory.ABUSE_RECLAIM, -pointAmount);
    }

    private int calculateActivityPoint(
            User user,
            PointCategory pointCategory,
            ActivityRewardPolicy activityRewardPolicy,
            BigDecimal amount
    ) {
        PointCalculationResult calculationResult = pointCalculatorService.calculateActivity(
                new PointCalculationCommand(
                        pointCategory,
                        activityRewardPolicy.getPointValue(),
                        activityRewardPolicy.getPointRate(),
                        amount
                ),
                user.getTotalPoints()
        );
        return calculationResult.appliedPoint();
    }

    private int calculateAndApplyBonus(User user, PointCategory pointCategory, LocalDateTime activityDateTime) {
        return switch (pointCategory) {
            case VOLUNTEER -> applyVolunteerMilestoneBonus(user);
            case PHOTO -> applyPhotoStreakBonus(user, activityDateTime);
            case QUIZ_CORRECT, QUIZ_WRONG -> applyMonthlyQuizBonus(user, activityDateTime);
            default -> 0;
        };
    }

    private int applyVolunteerMilestoneBonus(User user) {
        Optional<PointPolicy> pointPolicy = findActivePointPolicy(PointPolicyType.VOLUNTEER_MILESTONE);
        if (pointPolicy.isEmpty()) {
            return 0;
        }

        PointPolicy activePointPolicy = pointPolicy.get();
        long volunteerCount = userPointRepository.countByUserAndCategory(user, PointCategory.VOLUNTEER);

        if (activePointPolicy.getTargetCount() == null || activePointPolicy.getTargetCount() <= 0) {
            return 0;
        }
        if (volunteerCount % activePointPolicy.getTargetCount() != 0) {
            return 0;
        }

        savePointHistory(user, PointCategory.VOLUNTEER_MILESTONE_BONUS, activePointPolicy.getRewardPoint());
        return activePointPolicy.getRewardPoint();
    }

    private int applyPhotoStreakBonus(User user, LocalDateTime activityDateTime) {
        Optional<PointPolicy> pointPolicy = findActivePointPolicy(PointPolicyType.PHOTO_STREAK);
        if (pointPolicy.isEmpty()) {
            return 0;
        }

        PointPolicy activePointPolicy = pointPolicy.get();

        if (!isFirstPhotoOfDay(user, activityDateTime)) {
            return 0;
        }
        if (activePointPolicy.getTargetDays() == null || activePointPolicy.getTargetDays() <= 0) {
            return 0;
        }

        LocalDate today = activityDateTime.toLocalDate();
        LocalDateTime rangeStart = today.minusDays(activePointPolicy.getTargetDays() - 1L).atStartOfDay();
        LocalDateTime rangeEnd = today.plusDays(1L).atStartOfDay();

        Set<LocalDate> participatedDates = userPointRepository
                .findByUserAndCategoryAndCreatedAtBetweenOrderByCreatedAtAsc(
                        user,
                        PointCategory.PHOTO,
                        rangeStart,
                        rangeEnd
                )
                .stream()
                .map(userPoint -> userPoint.getCreatedAt().toLocalDate())
                .collect(Collectors.toSet());

        if (participatedDates.size() < activePointPolicy.getTargetDays()) {
            return 0;
        }

        for (int i = 0; i < activePointPolicy.getTargetDays(); i++) {
            if (!participatedDates.contains(today.minusDays(i))) {
                return 0;
            }
        }

        savePointHistory(user, PointCategory.PHOTO_STREAK_BONUS, activePointPolicy.getRewardPoint());
        return activePointPolicy.getRewardPoint();
    }

    private int applyMonthlyQuizBonus(User user, LocalDateTime activityDateTime) {
        Optional<PointPolicy> pointPolicy = findActivePointPolicy(PointPolicyType.QUIZ_MONTHLY);
        if (pointPolicy.isEmpty()) {
            return 0;
        }

        PointPolicy activePointPolicy = pointPolicy.get();

        LocalDate activityDate = activityDateTime.toLocalDate();
        YearMonth yearMonth = YearMonth.from(activityDate);
        if (activityDate.getDayOfMonth() != yearMonth.lengthOfMonth()) {
            return 0;
        }

        LocalDateTime startOfMonth = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

        Set<LocalDate> quizParticipationDates = userPointRepository
                .findByUserAndCategoryInAndCreatedAtBetweenOrderByCreatedAtAsc(
                        user,
                        QUIZ_CATEGORIES,
                        startOfMonth,
                        endOfMonth
                )
                .stream()
                .map(userPoint -> userPoint.getCreatedAt().toLocalDate())
                .collect(Collectors.toSet());

        if (quizParticipationDates.size() != yearMonth.lengthOfMonth()) {
            return 0;
        }

        savePointHistory(user, PointCategory.QUIZ_MONTHLY_BONUS, activePointPolicy.getRewardPoint());
        return activePointPolicy.getRewardPoint();
    }

    private boolean isFirstPhotoOfDay(User user, LocalDateTime activityDateTime) {
        LocalDateTime dayStart = activityDateTime.toLocalDate().atStartOfDay();
        LocalDateTime nextDayStart = dayStart.plusDays(1);

        List<UserPoint> photoPoints = userPointRepository.findByUserAndCategoryAndCreatedAtBetweenOrderByCreatedAtAsc(
                user,
                PointCategory.PHOTO,
                dayStart,
                nextDayStart
        );

        return photoPoints.size() == 1;
    }

    private int savePointHistory(User user, PointCategory category, int pointDelta) {
        user.applyPoint(pointDelta);
        userPointRepository.save(
                UserPoint.create(
                        user,
                        null,
                        category,
                        pointDelta,
                        user.getTotalPoints()
                )
        );
        return user.getTotalPoints();
    }

    private PointCategory mapToPointCategory(ActivityType activityType) {
        return switch (activityType) {
            case DONATION -> PointCategory.DONATION;
            case VOLUNTEER -> PointCategory.VOLUNTEER;
            case PURCHASE -> PointCategory.PURCHASE;
            case PHOTO -> PointCategory.PHOTO;
            case QUIZ_CORRECT -> PointCategory.QUIZ_CORRECT;
            case QUIZ_WRONG -> PointCategory.QUIZ_WRONG;
            case LOAN_REPAY -> null;
        };
    }

    private Optional<PointPolicy> findActivePointPolicy(PointPolicyType policyType) {
        return pointPolicyRepository.findByPolicyTypeAndIsActiveTrue(policyType);
    }

    private LocalDateTime resolveActivityDateTime(LocalDateTime activityDateTime) {
        return activityDateTime == null ? LocalDateTime.now(clock) : activityDateTime;
    }
}
