package com.shinhan.esg_be.domain.report.repository;

import com.shinhan.esg_be.domain.report.entity.ReportIssue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ReportIssueRepository extends JpaRepository<ReportIssue, Long> {

    boolean existsByCertificateNumber(String certificateNumber);

    boolean existsByVerificationCode(String verificationCode);

    boolean existsByVerificationToken(String verificationToken);

    Optional<ReportIssue> findByVerificationToken(String verificationToken);

    Optional<ReportIssue> findByReportIssueIdAndUser_UserId(Long reportIssueId, Long userId);

    long deleteByIssuedAtBefore(LocalDateTime cutoff);
}
