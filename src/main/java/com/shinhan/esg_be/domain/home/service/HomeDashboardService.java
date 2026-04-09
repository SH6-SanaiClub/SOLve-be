package com.shinhan.esg_be.domain.home.service;

import com.shinhan.esg_be.domain.home.dto.response.GradeProgressResponse;
import com.shinhan.esg_be.domain.home.dto.response.HomeDashboardSummaryResponse;
import com.shinhan.esg_be.domain.home.dto.response.WeeklyActivityStatusResponse;
import com.shinhan.esg_be.domain.score.entity.ValidScoreHistory;
import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.entity.enums.Grade;
import com.shinhan.esg_be.domain.user.repository.UserRepository;
import com.shinhan.esg_be.global.common.enums.ScoreReason;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeDashboardService {

    private static final int MAX_SCORE = 1000;

    private static final Set<ScoreReason> HOME_ACTIVITY_REASONS = EnumSet.of(
            ScoreReason.DONATION,
            ScoreReason.VOLUNTEER,
            ScoreReason.PURCHASE,
            ScoreReason.QUIZ,
            ScoreReason.PHOTO,
            ScoreReason.LOAN_REPAY
    );

    private final UserRepository userRepository;
    private final EntityManager entityManager;
    private final Clock clock;

    public HomeDashboardSummaryResponse getSummary(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "사용자 정보를 찾을 수 없습니다."));

        int totalScore = user.getTotalScore();
        Grade currentGrade = resolveCurrentGrade(totalScore);

        return new HomeDashboardSummaryResponse(
                user.getName(),
                currentGrade,
                user.getTotalPoints(),
                buildGradeProgress(totalScore, currentGrade),
                buildWeeklyActivities(userId)
        );
    }

    private GradeProgressResponse buildGradeProgress(int totalScore, Grade currentGrade) {
        int displayScore = Math.max(0, Math.min(totalScore, MAX_SCORE));
        int target = resolveNextThreshold(currentGrade);
        int segmentCurrent = switch (currentGrade) {
            case SEED -> Math.max(0, Math.min(displayScore, 600));
            case SPROUT -> Math.max(0, Math.min(displayScore - 600, 100));
            case TREE -> Math.max(0, Math.min(displayScore - 700, 100));
            case FOREST -> Math.max(0, Math.min(displayScore - 800, 100));
            case EARTH -> Math.max(0, Math.min(displayScore - 900, 100));
        };
        int segmentTarget = currentGrade == Grade.SEED ? 600 : 100;
        int visualValue = segmentTarget == 0
                ? 0
                : (int) Math.round((segmentCurrent * 100.0) / segmentTarget);

        return new GradeProgressResponse(
                displayScore,
                target,
                Math.min(100, Math.max(0, visualValue))
        );
    }

    private Grade resolveCurrentGrade(int totalScore) {
        if (totalScore >= 900) {
            return Grade.EARTH;
        }
        if (totalScore >= 800) {
            return Grade.FOREST;
        }
        if (totalScore >= 700) {
            return Grade.TREE;
        }
        if (totalScore >= 600) {
            return Grade.SPROUT;
        }
        return Grade.SEED;
    }

    private int resolveNextThreshold(Grade currentGrade) {
        return switch (currentGrade) {
            case SEED -> 600;
            case SPROUT -> 700;
            case TREE -> 800;
            case FOREST -> 900;
            case EARTH -> 1000;
        };
    }

    private List<WeeklyActivityStatusResponse> buildWeeklyActivities(Long userId) {
        LocalDate today = LocalDate.now(clock);
        LocalDate weekStartDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDateTime weekStart = weekStartDate.atStartOfDay();
        LocalDateTime weekEnd = weekStartDate.plusDays(7).atStartOfDay();

        Set<LocalDate> activityDates = entityManager.createQuery("""
                        select v
                        from ValidScoreHistory v
                        where v.user.userId = :userId
                          and v.reason in :reasons
                          and v.createdAt >= :weekStart
                          and v.createdAt < :weekEnd
                        """, ValidScoreHistory.class)
                .setParameter("userId", userId)
                .setParameter("reasons", HOME_ACTIVITY_REASONS)
                .setParameter("weekStart", weekStart)
                .setParameter("weekEnd", weekEnd)
                .getResultList()
                .stream()
                .map(ValidScoreHistory::getCreatedAt)
                .map(LocalDateTime::toLocalDate)
                .collect(Collectors.toSet());

        return List.of(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
                DayOfWeek.SATURDAY,
                DayOfWeek.SUNDAY
        ).stream()
                .map(dayOfWeek -> {
                    LocalDate targetDate = weekStartDate.with(TemporalAdjusters.nextOrSame(dayOfWeek));
                    return new WeeklyActivityStatusResponse(
                            toKoreanDayLabel(dayOfWeek),
                            activityDates.contains(targetDate)
                    );
                })
                .toList();
    }

    private String toKoreanDayLabel(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "월";
            case TUESDAY -> "화";
            case WEDNESDAY -> "수";
            case THURSDAY -> "목";
            case FRIDAY -> "금";
            case SATURDAY -> "토";
            case SUNDAY -> "일";
        };
    }
}
