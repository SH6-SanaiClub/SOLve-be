package com.shinhan.esg_be.domain.point.batch;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@ConditionalOnBean(name = "monthlyQuizBonusJob")
public class PointBatchScheduler {

    private final JobOperator jobOperator;
    private final Job monthlyQuizBonusJob;
    private final Clock clock;

    @Scheduled(cron = "0 5 0 1 * *", zone = "Asia/Seoul")
    public void runMonthlyQuizBonusJob() throws Exception {
        jobOperator.start(monthlyQuizBonusJob, buildJobParameters("monthlyQuizBonusJob"));
    }

    private JobParameters buildJobParameters(String jobName) {
        return new JobParametersBuilder()
                .addString("jobName", jobName)
                .addLocalDateTime("requestedAt", LocalDateTime.now(clock))
                .toJobParameters();
    }
}
