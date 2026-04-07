package com.shinhan.esg_be.domain.point.batch;

import com.shinhan.esg_be.domain.point.repository.UserPointRepository;
import com.shinhan.esg_be.domain.point.service.MonthlyPointService;
import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@ConditionalOnBean(UserPointRepository.class)
public class PointBatchConfig {

    private final UserRepository userRepository;
    private final MonthlyPointService monthlyPointService;
    private final Clock clock;

    @Bean
    public Job monthlyQuizBonusJob(JobRepository jobRepository, Step monthlyQuizBonusStep) {
        return new JobBuilder("monthlyQuizBonusJob", jobRepository)
                .start(monthlyQuizBonusStep)
                .build();
    }

    @Bean
    public Step monthlyQuizBonusStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager
    ) {
        return new StepBuilder("monthlyQuizBonusStep", jobRepository)
                .tasklet(monthlyQuizBonusTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet monthlyQuizBonusTasklet() {
        return (contribution, chunkContext) -> {
            LocalDateTime settledAt = LocalDateTime.now(clock);
            List<User> users = userRepository.findAll();
            for (User user : users) {
                monthlyPointService.settleMonthlyQuizBonus(user.getUserId(), settledAt);
            }
            return org.springframework.batch.infrastructure.repeat.RepeatStatus.FINISHED;
        };
    }
}
