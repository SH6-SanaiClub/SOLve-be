package com.shinhan.esg_be.domain.report.service;

import com.shinhan.esg_be.domain.report.dto.response.ReportCategorySummaryResponse;
import com.shinhan.esg_be.domain.report.dto.response.ReportCertificateMetaResponse;
import com.shinhan.esg_be.domain.report.dto.response.ReportCertificateSnapshotResponse;
import com.shinhan.esg_be.domain.report.dto.response.ReportVerificationResponse;
import com.shinhan.esg_be.domain.report.entity.ReportIssue;
import com.shinhan.esg_be.domain.report.repository.ReportIssueRepository;
import com.shinhan.esg_be.domain.user.entity.enums.Grade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportVerificationService {

    private static final String VERIFY_PATH_PREFIX = "/report/verify/";
    private final ReportIssueRepository reportIssueRepository;

    public ReportVerificationResponse verify(String token) {
        return reportIssueRepository.findByVerificationToken(token)
                .map(this::toVerificationResponse)
                .orElseGet(() -> new ReportVerificationResponse(false, "INVALID", null, null));
    }

    private ReportVerificationResponse toVerificationResponse(ReportIssue reportIssue) {
        boolean valid = reportIssue.getStatus() == ReportIssue.ReportIssueStatus.ACTIVE;
        String verificationStatus = valid ? "ACTIVE" : "REVOKED";

        return new ReportVerificationResponse(
                valid,
                verificationStatus,
                new ReportCertificateMetaResponse(
                        reportIssue.getReportIssueId(),
                        reportIssue.getCertificateNumber(),
                        reportIssue.getVerificationCode(),
                        VERIFY_PATH_PREFIX + reportIssue.getVerificationToken(),
                        reportIssue.getIssuedAt()
                ),
                new ReportCertificateSnapshotResponse(
                        reportIssue.getRecipientName(),
                        reportIssue.getPeriodType(),
                        resolvePeriodLabel(reportIssue.getPeriodType()),
                        reportIssue.getIssuedAt().toLocalDate(),
                        reportIssue.getTargetStartDate(),
                        reportIssue.getTargetEndDate(),
                        Grade.valueOf(reportIssue.getGrade()),
                        reportIssue.getTotalScore(),
                        reportIssue.getVerifiedActivityCount(),
                        reportIssue.getConsecutiveMaxAchievementMonths(),
                        reportIssue.getShowConsecutiveAchievementBadge(),
                        List.of(
                                new ReportCategorySummaryResponse("ENVIRONMENT", "환경 활동(E)", reportIssue.getEnvironmentActivityCount(), "건"),
                                new ReportCategorySummaryResponse("VOLUNTEER", "봉사활동(S)", reportIssue.getVolunteerHours(), "시간"),
                                new ReportCategorySummaryResponse("DONATION", "사회공헌 활동(S)", reportIssue.getSocialContributionCount(), "건"),
                                new ReportCategorySummaryResponse("TRUST", "신뢰 활동(G)", reportIssue.getTrustActivityCount(), "건")
                        )
                )
        );
    }

    private String resolvePeriodLabel(String periodType) {
        return switch (periodType) {
            case "1M" -> "최근 1개월";
            case "3M" -> "최근 3개월";
            case "6M" -> "최근 6개월";
            case "1Y" -> "최근 1년";
            default -> periodType;
        };
    }
}
