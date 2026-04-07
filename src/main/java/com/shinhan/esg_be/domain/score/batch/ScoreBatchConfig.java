package com.shinhan.esg_be.domain.score.batch;

import com.shinhan.esg_be.domain.score.service.MonthlyScoreService;
import com.shinhan.esg_be.domain.score.service.ScoreExpirationService;
import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class ScoreBatchConfig {

    private final UserRepository userRepository;
    private final MonthlyScoreService monthlyScoreService;
    private final ScoreExpirationService scoreExpirationService;
    private final Clock clock;

    @Bean
    public Job monthlyScoreSettlementJob(JobRepository jobRepository, Step monthlyScoreSettlementStep) {
        return new JobBuilder("monthlyScoreSettlementJob", jobRepository)
                .start(monthlyScoreSettlementStep)
                .build();
    }

    @Bean
    public Job scoreExpirationJob(JobRepository jobRepository, Step scoreExpirationStep) {
        return new JobBuilder("scoreExpirationJob", jobRepository)
                .start(scoreExpirationStep)
                .build();
    }

    @Bean
    public Step monthlyScoreSettlementStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager
    ) {
        return new StepBuilder("monthlyScoreSettlementStep", jobRepository)
                .tasklet(monthlyScoreSettlementTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Step scoreExpirationStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager
    ) {
        return new StepBuilder("scoreExpirationStep", jobRepository)
                .tasklet(scoreExpirationTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet monthlyScoreSettlementTasklet() {
        return (contribution, chunkContext) -> {
            LocalDateTime settledAt = LocalDateTime.now(clock);
            List<User> users = userRepository.findAll();
            for (User user : users) {
                monthlyScoreService.settleMonthlyScore(user.getUserId(), settledAt);
            }
            return org.springframework.batch.infrastructure.repeat.RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Tasklet scoreExpirationTasklet() {
        return (contribution, chunkContext) -> {
            LocalDateTime expiredAt = LocalDateTime.now(clock);
            List<User> users = userRepository.findAll();
            for (User user : users) {
                scoreExpirationService.expireUserScores(user.getUserId(), expiredAt);
            }
            return org.springframework.batch.infrastructure.repeat.RepeatStatus.FINISHED;
        };
    }
}
