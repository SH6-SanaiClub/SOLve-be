package com.shinhan.esg_be.domain.report.scheduler;

import com.shinhan.esg_be.domain.report.service.ReportIssueCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.scheduler.enabled", havingValue = "true")
public class ReportIssueCleanupScheduler {

    private final ReportIssueCleanupService reportIssueCleanupService;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void deleteExpiredReportIssues() {
        long deletedCount = reportIssueCleanupService.deleteExpiredIssues();
        log.info("Deleted {} expired report issues.", deletedCount);
    }
}
