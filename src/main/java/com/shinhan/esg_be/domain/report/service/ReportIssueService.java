package com.shinhan.esg_be.domain.report.service;

import com.shinhan.esg_be.domain.report.dto.request.ReportIssueRequest;
import com.shinhan.esg_be.domain.report.dto.response.ReportCertificateMetaResponse;
import com.shinhan.esg_be.domain.report.dto.response.ReportCertificateSnapshotResponse;
import com.shinhan.esg_be.domain.report.dto.response.ReportIssueResponse;
import com.shinhan.esg_be.domain.report.entity.ReportIssue;
import com.shinhan.esg_be.domain.report.repository.ReportIssueRepository;
import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.repository.UserRepository;
import com.shinhan.esg_be.global.config.ReportProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportIssueService {

    private static final DateTimeFormatter CERTIFICATE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyMMdd");
    private final UserRepository userRepository;
    private final ReportIssueRepository reportIssueRepository;
    private final MyReportService myReportService;
    private final ReportProperties reportProperties;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public ReportIssueResponse issue(Long userId, ReportIssueRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found."));

        ReportCertificateSnapshotResponse snapshot = myReportService.getPreviewSnapshot(userId, request.periodType());
        LocalDateTime issuedAt = LocalDateTime.now(clock);

        ReportIssue reportIssue = reportIssueRepository.save(
                ReportIssue.create(
                        user,
                        snapshot.periodType(),
                        generateCertificateNumber(issuedAt),
                        generateVerificationCode(),
                        generateVerificationToken(),
                        issuedAt,
                        snapshot.targetStartDate(),
                        snapshot.targetEndDate(),
                        snapshot.recipientName(),
                        snapshot.currentGrade().name(),
                        snapshot.totalScore(),
                        snapshot.verifiedActivityCount(),
                        snapshot.consecutiveMaxAchievementMonths(),
                        snapshot.showConsecutiveAchievementBadge(),
                        getCategoryValue(snapshot, "ENVIRONMENT"),
                        getCategoryValue(snapshot, "VOLUNTEER"),
                        getCategoryValue(snapshot, "DONATION"),
                        getCategoryValue(snapshot, "TRUST")
                )
        );

        return new ReportIssueResponse(
                toCertificateMeta(reportIssue),
                snapshot
        );
    }

    private int getCategoryValue(ReportCertificateSnapshotResponse snapshot, String categoryKey) {
        return snapshot.categories().stream()
                .filter(category -> categoryKey.equals(category.categoryKey()))
                .findFirst()
                .map(category -> category.value())
                .orElse(0);
    }

    private String generateCertificateNumber(LocalDateTime issuedAt) {
        String datePart = issuedAt.format(CERTIFICATE_DATE_FORMATTER);
        String candidate;
        do {
            candidate = "ESG-" + datePart + "-" + String.format("%04d", secureRandom.nextInt(10_000));
        } while (reportIssueRepository.existsByCertificateNumber(candidate));

        return candidate;
    }

    private String generateVerificationCode() {
        String candidate;
        do {
            candidate = "QR-" + randomAlphaNumeric(4) + "-" + randomAlphaNumeric(2);
        } while (reportIssueRepository.existsByVerificationCode(candidate));

        return candidate;
    }

    private String generateVerificationToken() {
        String candidate;
        do {
            byte[] bytes = new byte[24];
            secureRandom.nextBytes(bytes);
            candidate = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } while (reportIssueRepository.existsByVerificationToken(candidate));

        return candidate;
    }

    private String randomAlphaNumeric(int length) {
        final char[] source = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
        StringBuilder builder = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            builder.append(source[secureRandom.nextInt(source.length)]);
        }
        return builder.toString();
    }

    private ReportCertificateMetaResponse toCertificateMeta(ReportIssue reportIssue) {
        return new ReportCertificateMetaResponse(
                reportIssue.getReportIssueId(),
                reportIssue.getCertificateNumber(),
                reportIssue.getVerificationCode(),
                reportProperties.buildVerificationPath(reportIssue.getVerificationToken()),
                reportIssue.getIssuedAt()
        );
    }
}
