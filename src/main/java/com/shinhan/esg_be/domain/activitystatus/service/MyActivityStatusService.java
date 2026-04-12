package com.shinhan.esg_be.domain.activitystatus.service;

import com.shinhan.esg_be.domain.activitystatus.dto.response.ActivityStatusGradeHistoryItemResponse;
import com.shinhan.esg_be.domain.activitystatus.dto.response.ActivityStatusLogItemResponse;
import com.shinhan.esg_be.domain.activitystatus.dto.response.ActivityStatusLogResponse;
import com.shinhan.esg_be.domain.activitystatus.dto.response.ActivityStatusMonthlyScorePointResponse;
import com.shinhan.esg_be.domain.activitystatus.dto.response.ActivityStatusOverviewResponse;
import com.shinhan.esg_be.domain.activitystatus.dto.response.ActivityStatusSummaryResponse;
import com.shinhan.esg_be.domain.policy.entity.EsgScorePolicy;
import com.shinhan.esg_be.domain.policy.repository.EsgScorePolicyRepository;
import com.shinhan.esg_be.domain.activitystatus.repository.ActivityStatusQueryRepository;
import com.shinhan.esg_be.domain.score.entity.ExpiredScoreHistory;
import com.shinhan.esg_be.domain.score.entity.ValidScoreHistory;
import com.shinhan.esg_be.domain.stat.entity.UserMonthlyStat;
import com.shinhan.esg_be.domain.stat.repository.UserMonthlyStatRepository;
import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.entity.enums.Grade;
import com.shinhan.esg_be.domain.user.repository.UserRepository;
import com.shinhan.esg_be.global.common.enums.ScoreCategory;
import com.shinhan.esg_be.global.common.enums.ScoreReason;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyActivityStatusService {

    private static final int MAX_SCORE = 1000;
    private static final int OVERVIEW_MONTH_COUNT = 4;
    private static final int RECENT_MONTH_LOOKBACK_COUNT = 12;
    private static final int DEFAULT_LOG_SIZE = 10;
    private static final int MAX_LOG_SIZE = 50;
    private static final List<ScoreCategory> MONTHLY_TARGET_CATEGORIES = List.of(
            ScoreCategory.E,
            ScoreCategory.S,
            ScoreCategory.G_ACTIVITY
    );

    private static final Set<ScoreReason> ACTIVITY_REASONS = EnumSet.of(
            ScoreReason.DONATION,
            ScoreReason.VOLUNTEER,
            ScoreReason.PURCHASE,
            ScoreReason.QUIZ,
            ScoreReason.PHOTO,
            ScoreReason.LOAN_REPAY
    );

    private static final Set<ScoreReason> LOG_VISIBLE_REASONS = EnumSet.of(
            ScoreReason.DONATION,
            ScoreReason.VOLUNTEER,
            ScoreReason.PURCHASE,
            ScoreReason.QUIZ,
            ScoreReason.PHOTO,
            ScoreReason.LOAN_REPAY,
            ScoreReason.CONSECUTIVE_BONUS
    );

    private final UserRepository userRepository;
    private final ActivityStatusQueryRepository activityStatusQueryRepository;
    private final UserMonthlyStatRepository userMonthlyStatRepository;
    private final EsgScorePolicyRepository esgScorePolicyRepository;
    private final Clock clock;

    public ActivityStatusOverviewResponse getOverview(Long userId) {
        User user = getUser(userId);
        LocalDate today = LocalDate.now(clock);
        List<MonthScoreSnapshot> monthSnapshots = buildMonthSnapshots(user, today);

        return new ActivityStatusOverviewResponse(
                buildSummary(user, today),
                monthSnapshots.stream()
                        .map(this::toGradeHistoryItemResponse)
                        .toList(),
                monthSnapshots.stream()
                        .map(this::toMonthlyScorePointResponse)
                        .toList()
        );
    }

    public ActivityStatusLogResponse getLogs(
            Long userId,
            String category,
            Integer size,
            String cursor
    ) {
        getUser(userId);

        ActivityStatusFilter filter = ActivityStatusFilter.from(category);
        CursorPageRequest pageRequest = buildPageRequest(size, cursor);
        LocalDateTime from = LocalDate.now(clock).minusYears(1).atStartOfDay();

        List<ScoreCategory> categories = filter.toCategories();
        List<ValidScoreHistory> histories = activityStatusQueryRepository.findLogHistories(
                userId,
                from,
                LOG_VISIBLE_REASONS,
                categories,
                pageRequest.cursorOccurredAt(),
                pageRequest.cursorScoreId(),
                pageRequest.limit() + 1
        );

        boolean hasNext = histories.size() > pageRequest.limit();
        List<ValidScoreHistory> pageItems = hasNext
                ? histories.subList(0, pageRequest.limit())
                : histories;

        String nextCursor = hasNext && !pageItems.isEmpty()
                ? buildCursor(pageItems.get(pageItems.size() - 1))
                : null;

        long totalCount = activityStatusQueryRepository.countLogHistories(
                userId,
                from,
                LOG_VISIBLE_REASONS,
                categories
        );

        return new ActivityStatusLogResponse(
                Math.toIntExact(totalCount),
                hasNext,
                nextCursor,
                pageItems.stream()
                        .map(this::toLogItemResponse)
                        .toList()
        );
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found."));
    }

    private ActivityStatusSummaryResponse buildSummary(User user, LocalDate today) {
        ProgressSummary progressSummary = buildProgressSummary(user.getTotalScore());
        LocalDateTime currentMonthStart = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime nextMonthStart = currentMonthStart.plusMonths(1);

        int currentMonthActivityCount = activityStatusQueryRepository.findHistoriesByReasonsInPeriod(
                user.getUserId(),
                currentMonthStart,
                nextMonthStart,
                ACTIVITY_REASONS
        ).size();
        ConsecutiveAchievementBadgeSummary badgeSummary = buildConsecutiveAchievementBadgeSummary(user);

        return new ActivityStatusSummaryResponse(
                user.getCurrentGrade(),
                user.getTotalScore(),
                progressSummary.current(),
                progressSummary.target(),
                progressSummary.percent(),
                currentMonthActivityCount,
                badgeSummary.consecutiveMonths(),
                badgeSummary.visible(),
                today
        );
    }

    private List<MonthScoreSnapshot> buildMonthSnapshots(User user, LocalDate today) {
        YearMonth currentMonth = YearMonth.from(today);
        List<YearMonth> months = new ArrayList<>();
        for (int index = OVERVIEW_MONTH_COUNT - 1; index >= 0; index--) {
            months.add(currentMonth.minusMonths(index));
        }

        LocalDateTime from = months.get(0).atDay(1).atStartOfDay();
        LocalDateTime to = currentMonth.plusMonths(1).atDay(1).atStartOfDay();

        List<ValidScoreHistory> histories = activityStatusQueryRepository.findHistoriesInPeriod(
                user.getUserId(),
                from,
                to
        );
        List<ExpiredScoreHistory> expiredHistories = activityStatusQueryRepository.findExpiredHistoriesByValidUntilPeriod(
                user.getUserId(),
                from,
                to
        );

        Map<YearMonth, Integer> monthlyNetDeltaByMonth = new HashMap<>();
        for (ValidScoreHistory history : histories) {
            YearMonth yearMonth = YearMonth.from(history.getCreatedAt());
            monthlyNetDeltaByMonth.merge(yearMonth, history.getChangeAmount(), Integer::sum);
        }
        for (ExpiredScoreHistory history : expiredHistories) {
            YearMonth yearMonth = YearMonth.from(history.getValidUntil());
            monthlyNetDeltaByMonth.merge(yearMonth, -history.getChangeAmount(), Integer::sum);
        }

        Map<YearMonth, Integer> monthEndScoreByMonth = new HashMap<>();
        int rollingScore = user.getTotalScore();
        for (int index = months.size() - 1; index >= 0; index--) {
            YearMonth yearMonth = months.get(index);
            monthEndScoreByMonth.put(yearMonth, rollingScore);
            rollingScore -= monthlyNetDeltaByMonth.getOrDefault(yearMonth, 0);
        }

        return months.stream()
                .map(yearMonth -> new MonthScoreSnapshot(
                        yearMonth.getYear(),
                        yearMonth.getMonthValue(),
                        monthEndScoreByMonth.getOrDefault(yearMonth, user.getTotalScore()),
                        yearMonth.equals(currentMonth)
                ))
                .toList();
    }

    private ConsecutiveAchievementBadgeSummary buildConsecutiveAchievementBadgeSummary(User user) {
        UserMonthlyStat userMonthlyStat = userMonthlyStatRepository.findByUser(user)
                .orElse(null);
        if (userMonthlyStat == null) {
            return new ConsecutiveAchievementBadgeSummary(0, false);
        }

        Map<ScoreCategory, Integer> monthlyMaxScoreByCategory = loadMonthlyMaxScoreByCategory();
        boolean achievedAllMonthlyTargets = MONTHLY_TARGET_CATEGORIES.stream()
                .allMatch(category -> userMonthlyStat.getMonthlyScore(category) >= monthlyMaxScoreByCategory.get(category));

        if (!achievedAllMonthlyTargets) {
            return new ConsecutiveAchievementBadgeSummary(0, false);
        }

        int consecutiveMonths = Math.min(
                userMonthlyStat.getConsecutiveEMaxScore(),
                Math.min(
                        userMonthlyStat.getConsecutiveSMaxScore(),
                        userMonthlyStat.getConsecutiveGMaxScore()
                )
        ) + 1;

        return new ConsecutiveAchievementBadgeSummary(consecutiveMonths, consecutiveMonths > 0);
    }

    private Map<ScoreCategory, Integer> loadMonthlyMaxScoreByCategory() {
        Map<ScoreCategory, Integer> monthlyMaxScoreByCategory = new HashMap<>();
        for (ScoreCategory category : MONTHLY_TARGET_CATEGORIES) {
            EsgScorePolicy policy = esgScorePolicyRepository.findByCategoryAndIsActiveTrue(category)
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "ESG score policy not found."));
            monthlyMaxScoreByCategory.put(category, defaultIfNull(policy.getMonthlyMaxScore()));
        }
        return monthlyMaxScoreByCategory;
    }

    private ProgressSummary buildProgressSummary(int totalScore) {
        int displayScore = Math.max(0, Math.min(totalScore, MAX_SCORE));
        int target = MAX_SCORE;
        int current = displayScore;
        int percent = (int) Math.round((current * 100.0) / target);

        return new ProgressSummary(current, target, Math.max(0, Math.min(percent, 100)));
    }

    private Grade resolveGrade(int totalScore) {
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

    private ActivityStatusGradeHistoryItemResponse toGradeHistoryItemResponse(MonthScoreSnapshot snapshot) {
        return new ActivityStatusGradeHistoryItemResponse(
                snapshot.year(),
                snapshot.month(),
                resolveGrade(snapshot.totalScore()),
                snapshot.totalScore(),
                snapshot.currentMonth()
        );
    }

    private ActivityStatusMonthlyScorePointResponse toMonthlyScorePointResponse(MonthScoreSnapshot snapshot) {
        return new ActivityStatusMonthlyScorePointResponse(
                snapshot.year(),
                snapshot.month(),
                snapshot.totalScore(),
                snapshot.currentMonth()
        );
    }

    private ActivityStatusLogItemResponse toLogItemResponse(ValidScoreHistory history) {
        return new ActivityStatusLogItemResponse(
                history.getScoreId(),
                resolveLogTitle(history.getReason()),
                toResponseCategory(history.getCategory()),
                history.getReason(),
                history.getChangeAmount(),
                history.getScoreAfter(),
                history.getCreatedAt()
        );
    }

    private String resolveLogTitle(ScoreReason scoreReason) {
        return switch (scoreReason) {
            case DONATION -> "기부 참여";
            case VOLUNTEER -> "봉사 활동 참여";
            case PURCHASE -> "친환경 제품 구매";
            case QUIZ -> "ESG 퀴즈 참여";
            case PHOTO -> "친환경 인증";
            case LOAN_REPAY -> "대출 상환";
            case CONSECUTIVE_BONUS -> "연속 활동 보너스";
            case ABUSE -> "부정 이용 패널티";
            case NO_ACTIVITY -> "미활동 패널티";
            case INITIAL_SCORE -> "초기 점수";
        };
    }

    private String toResponseCategory(ScoreCategory scoreCategory) {
        return switch (scoreCategory) {
            case E -> "E";
            case S -> "S";
            case G_ACTIVITY, G_REPAYMENT -> "G";
        };
    }

    private CursorPageRequest buildPageRequest(Integer size, String cursor) {
        int resolvedSize = size == null ? DEFAULT_LOG_SIZE : size;
        if (resolvedSize <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "size must be greater than 0.");
        }

        int limit = Math.min(resolvedSize, MAX_LOG_SIZE);
        LogCursor parsedCursor = parseCursor(cursor);
        return new CursorPageRequest(limit, parsedCursor.occurredAt(), parsedCursor.scoreId());
    }

    private LogCursor parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return new LogCursor(null, null);
        }

        String[] tokens = cursor.split("\\|");
        if (tokens.length != 2) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid cursor format.");
        }

        try {
            return new LogCursor(LocalDateTime.parse(tokens[0]), Long.parseLong(tokens[1]));
        } catch (DateTimeParseException | NumberFormatException exception) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid cursor format.");
        }
    }

    private String buildCursor(ValidScoreHistory history) {
        return history.getCreatedAt() + "|" + history.getScoreId();
    }

    private int defaultIfNull(Integer value) {
        return value == null ? 0 : value;
    }

    private record MonthScoreSnapshot(
            Integer year,
            Integer month,
            Integer totalScore,
            Boolean currentMonth
    ) {
    }

    private record ProgressSummary(
            Integer current,
            Integer target,
            Integer percent
    ) {
    }

    private record ConsecutiveAchievementBadgeSummary(
            Integer consecutiveMonths,
            Boolean visible
    ) {
    }

    private record CursorPageRequest(
            int limit,
            LocalDateTime cursorOccurredAt,
            Long cursorScoreId
    ) {
    }

    private record LogCursor(
            LocalDateTime occurredAt,
            Long scoreId
    ) {
    }

    private enum ActivityStatusFilter {
        ALL,
        E,
        S,
        G;

        static ActivityStatusFilter from(String value) {
            if (value == null || value.isBlank()) {
                return ALL;
            }

            try {
                return ActivityStatusFilter.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException exception) {
                throw new ResponseStatusException(BAD_REQUEST, "Invalid category value.");
            }
        }

        List<ScoreCategory> toCategories() {
            return switch (this) {
                case ALL -> List.of();
                case E -> List.of(ScoreCategory.E);
                case S -> List.of(ScoreCategory.S);
                case G -> List.of(ScoreCategory.G_ACTIVITY, ScoreCategory.G_REPAYMENT);
            };
        }
    }
}
