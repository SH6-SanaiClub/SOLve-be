package com.shinhan.esg_be.domain.report.entity;

import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "report_issue")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReportIssue extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_issue_id")
    private Long reportIssueId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "period_type", nullable = false, length = 2)
    private String periodType;

    @Column(name = "certificate_number", nullable = false, unique = true, length = 50)
    private String certificateNumber;

    @Column(name = "verification_code", nullable = false, unique = true, length = 50)
    private String verificationCode;

    @Column(name = "verification_token", nullable = false, unique = true, length = 128)
    private String verificationToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportIssueStatus status = ReportIssueStatus.ACTIVE;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "target_start_date", nullable = false)
    private LocalDate targetStartDate;

    @Column(name = "target_end_date", nullable = false)
    private LocalDate targetEndDate;

    @Column(name = "recipient_name", nullable = false, length = 100)
    private String recipientName;

    @Column(name = "grade", nullable = false, length = 20)
    private String grade;

    @Column(name = "total_score", nullable = false)
    private Integer totalScore;

    @Column(name = "verified_activity_count", nullable = false)
    private Integer verifiedActivityCount;

    @Column(name = "consecutive_max_achievement_months", nullable = false)
    private Integer consecutiveMaxAchievementMonths = 0;

    @Column(name = "show_consecutive_achievement_badge", nullable = false)
    private Boolean showConsecutiveAchievementBadge = false;

    @Column(name = "environment_activity_count", nullable = false)
    private Integer environmentActivityCount = 0;

    @Column(name = "volunteer_hours", nullable = false)
    private Integer volunteerHours = 0;

    @Column(name = "social_contribution_count", nullable = false)
    private Integer socialContributionCount = 0;

    @Column(name = "trust_activity_count", nullable = false)
    private Integer trustActivityCount = 0;

    public static ReportIssue create(
            User user,
            String periodType,
            String certificateNumber,
            String verificationCode,
            String verificationToken,
            LocalDateTime issuedAt,
            LocalDate targetStartDate,
            LocalDate targetEndDate,
            String recipientName,
            String grade,
            Integer totalScore,
            Integer verifiedActivityCount,
            Integer consecutiveMaxAchievementMonths,
            Boolean showConsecutiveAchievementBadge,
            Integer environmentActivityCount,
            Integer volunteerHours,
            Integer socialContributionCount,
            Integer trustActivityCount
    ) {
        ReportIssue reportIssue = new ReportIssue();
        reportIssue.user = user;
        reportIssue.periodType = periodType;
        reportIssue.certificateNumber = certificateNumber;
        reportIssue.verificationCode = verificationCode;
        reportIssue.verificationToken = verificationToken;
        reportIssue.status = ReportIssueStatus.ACTIVE;
        reportIssue.issuedAt = issuedAt;
        reportIssue.targetStartDate = targetStartDate;
        reportIssue.targetEndDate = targetEndDate;
        reportIssue.recipientName = recipientName;
        reportIssue.grade = grade;
        reportIssue.totalScore = totalScore;
        reportIssue.verifiedActivityCount = verifiedActivityCount;
        reportIssue.consecutiveMaxAchievementMonths = defaultIfNull(consecutiveMaxAchievementMonths);
        reportIssue.showConsecutiveAchievementBadge = defaultIfNull(showConsecutiveAchievementBadge);
        reportIssue.environmentActivityCount = defaultIfNull(environmentActivityCount);
        reportIssue.volunteerHours = defaultIfNull(volunteerHours);
        reportIssue.socialContributionCount = defaultIfNull(socialContributionCount);
        reportIssue.trustActivityCount = defaultIfNull(trustActivityCount);
        return reportIssue;
    }

    public void revoke() {
        this.status = ReportIssueStatus.REVOKED;
    }

    private static int defaultIfNull(Integer value) {
        return value == null ? 0 : value;
    }

    private static boolean defaultIfNull(Boolean value) {
        return value != null && value;
    }

    public enum ReportIssueStatus {
        ACTIVE,
        REVOKED
    }
}
