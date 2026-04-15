package com.shinhan.esg_be.domain.bank.batch;

import com.shinhan.esg_be.domain.bank.service.LoanRepaymentBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.batch.enabled", havingValue = "true")
public class LoanBatchConfig {

    private final LoanRepaymentBatchService loanRepaymentBatchService;

    @Bean
    public Job autoLoanRepaymentJob(JobRepository jobRepository, Step autoLoanRepaymentStep) {
        return new JobBuilder("autoLoanRepaymentJob", jobRepository)
                .start(autoLoanRepaymentStep)
                .build();
    }

    @Bean
    public Step autoLoanRepaymentStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager
    ) {
        return new StepBuilder("autoLoanRepaymentStep", jobRepository)
                .tasklet(autoLoanRepaymentTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet autoLoanRepaymentTasklet() {
        return (contribution, chunkContext) -> {
            loanRepaymentBatchService.processDueRepayments();
            return org.springframework.batch.infrastructure.repeat.RepeatStatus.FINISHED;
        };
    }
}
