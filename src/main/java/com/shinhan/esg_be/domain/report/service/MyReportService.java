package com.shinhan.esg_be.domain.report.service;

import com.shinhan.esg_be.domain.policy.entity.EsgScorePolicy;
import com.shinhan.esg_be.domain.policy.repository.EsgScorePolicyRepository;
import com.shinhan.esg_be.domain.report.entity.enums.ReportPeriodType;
import com.shinhan.esg_be.domain.report.dto.response.ReportCategorySummaryResponse;
import com.shinhan.esg_be.domain.report.dto.response.ReportCertificateSnapshotResponse;
import com.shinhan.esg_be.domain.report.dto.response.ReportPreviewResponse;
import com.shinhan.esg_be.domain.report.repository.ReportQueryRepository;
import com.shinhan.esg_be.domain.stat.entity.UserMonthlyStat;
import com.shinhan.esg_be.domain.stat.repository.UserMonthlyStatRepository;
import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.repository.UserRepository;
import com.shinhan.esg_be.global.common.enums.ScoreCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyReportService {

    private static final List<ScoreCategory> MONTHLY_TARGET_CATEGORIES = List.of(
            ScoreCategory.E,
            ScoreCategory.S,
            ScoreCategory.G_ACTIVITY
    );

    private final UserRepository userRepository;
    private final UserMonthlyStatRepository userMonthlyStatRepository;
    private final EsgScorePolicyRepository esgScorePolicyRepository;
    private final ReportQueryRepository reportQueryRepository;
    private final Clock clock;

    public ReportPreviewResponse getPreview(Long userId, String periodTypeCode) {
        return new ReportPreviewResponse(getPreviewSnapshot(userId, periodTypeCode));
    }

    public ReportCertificateSnapshotResponse getPreviewSnapshot(Long userId, String periodTypeCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found."));

        ReportPeriodType periodType = ReportPeriodType.from(periodTypeCode);
        LocalDate referenceDate = LocalDate.now(clock);
        LocalDateTime from = periodType.startDateTime(referenceDate);
        LocalDateTime to = periodType.endDateTime(referenceDate);
        LocalDate requestedStartDate = from.toLocalDate();
        LocalDate firstActivityDate = reportQueryRepository.findFirstActivityDate(userId);
        LocalDate targetStartDate = firstActivityDate != null && firstActivityDate.isAfter(requestedStartDate)
                ? firstActivityDate
                : requestedStartDate;

        long environmentActivityCount = reportQueryRepository.countApprovedEnvironmentActivities(userId, from, to);
        long completedVolunteerCount = reportQueryRepository.countCompletedVolunteerActivities(userId, from, to);
        int completedVolunteerHours = reportQueryRepository.sumCompletedVolunteerHours(userId, from, to);
        long donationCount = reportQueryRepository.countDonations(userId, from, to);
        long ecoProductPurchaseCount = reportQueryRepository.countEcoProductPurchases(userId, from, to);
        long quizActivityCount = reportQueryRepository.countQuizActivities(userId, from, to);

        ConsecutiveAchievementBadgeSummary badgeSummary = buildConsecutiveAchievementBadgeSummary(user);

        return new ReportCertificateSnapshotResponse(
                user.getName(),
                periodType.code(),
                periodType.label(),
                referenceDate,
                targetStartDate,
                referenceDate,
                user.getCurrentGrade(),
                user.getTotalScore(),
                Math.toIntExact(
                        environmentActivityCount
                                + completedVolunteerCount
                                + donationCount
                                + ecoProductPurchaseCount
                                + quizActivityCount
                ),
                badgeSummary.consecutiveMonths(),
                badgeSummary.visible(),
                List.of(
                        new ReportCategorySummaryResponse("ENVIRONMENT", "환경 활동(E)", Math.toIntExact(environmentActivityCount), "건"),
                        new ReportCategorySummaryResponse("VOLUNTEER", "봉사활동(S)", completedVolunteerHours, "시간"),
                        new ReportCategorySummaryResponse("DONATION", "사회공헌 활동(S)", Math.toIntExact(donationCount), "건"),
                        new ReportCategorySummaryResponse("TRUST", "신뢰 활동(G)", Math.toIntExact(quizActivityCount), "건")
                )
        );
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

    private int defaultIfNull(Integer value) {
        return value == null ? 0 : value;
    }

    private record ConsecutiveAchievementBadgeSummary(
            Integer consecutiveMonths,
            Boolean visible
    ) {
    }
}
