package com.shinhan.esg_be.domain.bank.batch;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@ConditionalOnBean(name = "autoSavingPaymentJob")
@ConditionalOnProperty(name = "app.batch.enabled", havingValue = "true")
public class SavingBatchScheduler {

    private final JobOperator jobOperator;
    private final Job autoSavingPaymentJob;
    private final Clock clock;

    @Scheduled(cron = "0 5 1 * * *", zone = "Asia/Seoul")
    public void runAutoSavingPaymentJob() throws Exception {
        jobOperator.start(autoSavingPaymentJob, buildJobParameters("autoSavingPaymentJob"));
    }

    private JobParameters buildJobParameters(String jobName) {
        return new JobParametersBuilder()
                .addString("jobName", jobName)
                .addLocalDateTime("requestedAt", LocalDateTime.now(clock))
                .toJobParameters();
    }
}
