package com.shinhan.esg_be.domain.bank.batch;

import com.shinhan.esg_be.domain.bank.service.SavingPaymentBatchService;
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

@Configuration
@RequiredArgsConstructor
public class SavingBatchConfig {

    private final SavingPaymentBatchService savingPaymentBatchService;

    @Bean
    public Job autoSavingPaymentJob(JobRepository jobRepository, Step autoSavingPaymentStep) {
        return new JobBuilder("autoSavingPaymentJob", jobRepository)
                .start(autoSavingPaymentStep)
                .build();
    }

    @Bean
    public Step autoSavingPaymentStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager
    ) {
        return new StepBuilder("autoSavingPaymentStep", jobRepository)
                .tasklet(autoSavingPaymentTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet autoSavingPaymentTasklet() {
        return (contribution, chunkContext) -> {
            savingPaymentBatchService.processDuePayments();
            return org.springframework.batch.infrastructure.repeat.RepeatStatus.FINISHED;
        };
    }
}
