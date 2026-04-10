package com.shinhan.esg_be.domain.bank.service;

import com.shinhan.esg_be.domain.bank.entity.FinancialProduct;
import com.shinhan.esg_be.domain.bank.entity.LoanHistory;
import com.shinhan.esg_be.domain.bank.entity.UserLoan;
import com.shinhan.esg_be.domain.bank.entity.enums.LoanStatus;
import com.shinhan.esg_be.domain.bank.entity.enums.ProductType;
import com.shinhan.esg_be.domain.bank.repository.LoanHistoryRepository;
import com.shinhan.esg_be.domain.bank.repository.UserLoanRepository;
import com.shinhan.esg_be.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LoanRepaymentBatchServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-04-10T00:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    @InjectMocks
    private LoanRepaymentBatchService loanRepaymentBatchService;

    @Mock
    private UserLoanRepository userLoanRepository;

    @Mock
    private LoanHistoryRepository loanHistoryRepository;

    @Mock
    private Clock clock;

    @Test
    @DisplayName("자동 대출 상환은 일반 회차에 월 이자 이력만 생성하고 다음 상환일을 갱신한다")
    void processDueRepaymentsMonthlyInterestOnly() {
        UserLoan userLoan = createUserLoan(1L, LocalDate.of(2026, 4, 10), LocalDateTime.of(2026, 3, 10, 0, 0));

        given(clock.getZone()).willReturn(FIXED_CLOCK.getZone());
        given(clock.instant()).willReturn(FIXED_CLOCK.instant());
        given(userLoanRepository.findByStatusAndNextRepaymentDateLessThanEqual(LoanStatus.ACTIVE, LocalDate.of(2026, 4, 10)))
                .willReturn(List.of(userLoan));
        given(loanHistoryRepository.countByUserLoan_LoanId(1L)).willReturn(0L);

        loanRepaymentBatchService.processDueRepayments();

        ArgumentCaptor<LoanHistory> captor = ArgumentCaptor.forClass(LoanHistory.class);
        verify(loanHistoryRepository).save(captor.capture());
        LoanHistory savedHistory = captor.getValue();

        assertThat(savedHistory.getAmount()).isEqualTo(8_750L);
        assertThat(savedHistory.getPaymentDate()).isEqualTo(LocalDateTime.of(2026, 4, 10, 9, 0));
        assertThat(userLoan.getStatus()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(userLoan.getNextRepaymentDate()).isEqualTo(LocalDate.of(2026, 5, 10));
    }

    @Test
    @DisplayName("자동 대출 상환은 마지막 회차에 원금과 이자를 함께 상환하고 대출을 종료한다")
    void processDueRepaymentsFinalInstallment() {
        UserLoan userLoan = createUserLoan(1L, LocalDate.of(2027, 3, 10), LocalDateTime.of(2026, 3, 10, 0, 0));

        given(clock.getZone()).willReturn(FIXED_CLOCK.getZone());
        given(clock.instant()).willReturn(FIXED_CLOCK.instant());
        given(userLoanRepository.findByStatusAndNextRepaymentDateLessThanEqual(LoanStatus.ACTIVE, LocalDate.of(2026, 4, 10)))
                .willReturn(List.of(userLoan));
        given(loanHistoryRepository.countByUserLoan_LoanId(1L)).willReturn(11L);

        loanRepaymentBatchService.processDueRepayments();

        ArgumentCaptor<LoanHistory> captor = ArgumentCaptor.forClass(LoanHistory.class);
        verify(loanHistoryRepository).save(captor.capture());
        LoanHistory savedHistory = captor.getValue();

        assertThat(savedHistory.getAmount()).isEqualTo(1_008_750L);
        assertThat(userLoan.getStatus()).isEqualTo(LoanStatus.COMPLETE);
        assertThat(userLoan.getNextRepaymentDate()).isEqualTo(LocalDate.of(2027, 3, 10));
    }

    private UserLoan createUserLoan(Long loanId, LocalDate nextRepaymentDate, LocalDateTime createdAt) {
        User user = newInstance(User.class);
        FinancialProduct product = newInstance(FinancialProduct.class);
        ReflectionTestUtils.setField(product, "type", ProductType.LOAN);

        UserLoan userLoan = newInstance(UserLoan.class);
        ReflectionTestUtils.setField(userLoan, "loanId", loanId);
        ReflectionTestUtils.setField(userLoan, "user", user);
        ReflectionTestUtils.setField(userLoan, "financialProduct", product);
        ReflectionTestUtils.setField(userLoan, "principalAmount", 1_000_000L);
        ReflectionTestUtils.setField(userLoan, "currentRate", new BigDecimal("10.50"));
        ReflectionTestUtils.setField(userLoan, "status", LoanStatus.ACTIVE);
        ReflectionTestUtils.setField(userLoan, "totalAmount", 1_105_000L);
        ReflectionTestUtils.setField(userLoan, "nextRepaymentDate", nextRepaymentDate);
        ReflectionTestUtils.setField(userLoan, "baseEsgScore", 800);
        ReflectionTestUtils.setField(userLoan, "createdAt", createdAt);
        return userLoan;
    }

    private <T> T newInstance(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to instantiate " + type.getSimpleName(), exception);
        }
    }
}
