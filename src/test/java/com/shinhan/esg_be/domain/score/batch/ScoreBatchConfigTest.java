package com.shinhan.esg_be.domain.score.batch;

import com.shinhan.esg_be.domain.bank.service.SavingPrimeSettlementService;
import com.shinhan.esg_be.domain.score.service.MonthlyScoreService;
import com.shinhan.esg_be.domain.score.service.ScoreExpirationService;
import com.shinhan.esg_be.domain.score.service.ScorePenaltyService;
import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.core.step.StepContribution;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScoreBatchConfigTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MonthlyScoreService monthlyScoreService;

    @Mock
    private ScoreExpirationService scoreExpirationService;

    @Mock
    private ScorePenaltyService scorePenaltyService;

    @Mock
    private SavingPrimeSettlementService savingPrimeSettlementService;

    @Test
    void monthlyScoreSettlementTaskletSettlesPrimeBeforeMonthlyScoreReset() throws Exception {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-05-01T00:01:00Z"), ZoneId.of("Asia/Seoul"));
        ScoreBatchConfig scoreBatchConfig = new ScoreBatchConfig(
                userRepository,
                monthlyScoreService,
                scoreExpirationService,
                scorePenaltyService,
                savingPrimeSettlementService,
                fixedClock
        );

        User firstUser = mock(User.class);
        when(firstUser.getUserId()).thenReturn(1L);
        User secondUser = mock(User.class);
        when(secondUser.getUserId()).thenReturn(2L);
        when(userRepository.findAll()).thenReturn(List.of(firstUser, secondUser));

        Tasklet tasklet = scoreBatchConfig.monthlyScoreSettlementTasklet();
        tasklet.execute(mock(StepContribution.class), mock(ChunkContext.class));

        ArgumentCaptor<LocalDateTime> settledAtCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        InOrder inOrder = inOrder(savingPrimeSettlementService, monthlyScoreService);
        inOrder.verify(savingPrimeSettlementService).settleMonthlyPrimeRates(settledAtCaptor.capture());
        LocalDateTime settledAt = settledAtCaptor.getValue();
        inOrder.verify(monthlyScoreService).settleMonthlyScore(1L, settledAt);
        inOrder.verify(monthlyScoreService).settleMonthlyScore(2L, settledAt);
        verifyNoMoreInteractions(savingPrimeSettlementService, monthlyScoreService);
    }
}
