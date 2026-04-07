package com.shinhan.esg_be.domain.score.batch;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ScoreBatchScheduler {

    private final JobOperator jobOperator;
    private final Job monthlyScoreSettlementJob;
    private final Job noActivityPenaltyJob;
    private final Job scoreExpirationJob;
    private final Clock clock;

    @Scheduled(cron = "0 1 0 1 * *", zone = "Asia/Seoul")
    public void runMonthlyScoreSettlementJob() throws Exception {
        jobOperator.start(monthlyScoreSettlementJob, buildJobParameters("monthlyScoreSettlementJob"));
    }

    @Scheduled(cron = "0 7 0 1 * *", zone = "Asia/Seoul")
    public void runNoActivityPenaltyJob() throws Exception {
        jobOperator.start(noActivityPenaltyJob, buildJobParameters("noActivityPenaltyJob"));
    }

    @Scheduled(cron = "0 10 0 * * *", zone = "Asia/Seoul")
    public void runScoreExpirationJob() throws Exception {
        jobOperator.start(scoreExpirationJob, buildJobParameters("scoreExpirationJob"));
    }

    private JobParameters buildJobParameters(String jobName) {
        return new JobParametersBuilder()
                .addString("jobName", jobName)
                .addLocalDateTime("requestedAt", LocalDateTime.now(clock))
                .toJobParameters();
    }
}
