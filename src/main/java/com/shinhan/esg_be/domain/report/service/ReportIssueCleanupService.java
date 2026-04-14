package com.shinhan.esg_be.domain.report.service;

import com.shinhan.esg_be.domain.report.repository.ReportIssueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReportIssueCleanupService {

    private final ReportIssueRepository reportIssueRepository;
    private final Clock clock;

    @Transactional
    public long deleteExpiredIssues() {
        LocalDateTime cutoff = LocalDateTime.now(clock).minusMonths(1);
        return reportIssueRepository.deleteByIssuedAtBefore(cutoff);
    }
}
