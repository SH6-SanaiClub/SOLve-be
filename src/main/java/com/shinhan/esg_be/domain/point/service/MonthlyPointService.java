package com.shinhan.esg_be.domain.point.service;

import com.shinhan.esg_be.domain.point.entity.UserPoint;
import com.shinhan.esg_be.domain.point.entity.enums.PointReason;
import com.shinhan.esg_be.domain.point.repository.UserPointRepository;
import com.shinhan.esg_be.domain.policy.entity.PointPolicy;
import com.shinhan.esg_be.domain.policy.entity.enums.PointPolicyType;
import com.shinhan.esg_be.domain.policy.repository.PointPolicyRepository;
import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MonthlyPointService {

    private static final Set<PointReason> QUIZ_REASONS = EnumSet.of(PointReason.QUIZ_CORRECT, PointReason.QUIZ_WRONG);

    private final UserRepository userRepository;
    private final PointPolicyRepository pointPolicyRepository;
    private final UserPointRepository userPointRepository;

    @Transactional
    public int settleMonthlyQuizBonus(Long userId, LocalDateTime settledAt) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        Optional<PointPolicy> pointPolicy = pointPolicyRepository.findByPolicyTypeAndIsActiveTrue(PointPolicyType.QUIZ_MONTHLY);
        if (pointPolicy.isEmpty()) {
            return 0;
        }

        PointPolicy activePointPolicy = pointPolicy.get();
        LocalDateTime baseDateTime = settledAt == null ? LocalDateTime.now() : settledAt;
        YearMonth targetMonth = YearMonth.from(baseDateTime.toLocalDate().minusMonths(1));
        LocalDateTime startOfMonth = targetMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = targetMonth.plusMonths(1).atDay(1).atStartOfDay();

        boolean alreadyGranted = userPointRepository.existsByUserAndReasonAndCreatedAtBetween(
                user,
                PointReason.QUIZ_MONTHLY_BONUS,
                startOfMonth,
                endOfMonth
        );
        if (alreadyGranted) {
            return 0;
        }

        Set<LocalDate> quizParticipationDates = userPointRepository
                .findByUserAndReasonInAndCreatedAtBetweenOrderByCreatedAtAsc(
                        user,
                        QUIZ_REASONS,
                        startOfMonth,
                        endOfMonth
                )
                .stream()
                .map(UserPoint::getCreatedAt)
                .map(LocalDateTime::toLocalDate)
                .collect(Collectors.toSet());

        if (quizParticipationDates.size() != targetMonth.lengthOfMonth()) {
            return 0;
        }

        user.applyPoint(activePointPolicy.getRewardPoint());
        userPointRepository.save(
                UserPoint.create(
                        user,
                        null,
                        PointReason.QUIZ_MONTHLY_BONUS,
                        activePointPolicy.getRewardPoint(),
                        user.getTotalPoints()
                )
        );
        return activePointPolicy.getRewardPoint();
    }
}
