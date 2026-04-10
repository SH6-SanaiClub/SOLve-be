package com.shinhan.esg_be.domain.bank.service;

import com.shinhan.esg_be.domain.bank.entity.FinancialProduct;
import com.shinhan.esg_be.domain.bank.entity.SavingHistory;
import com.shinhan.esg_be.domain.bank.entity.UserSaving;
import com.shinhan.esg_be.domain.bank.entity.enums.ProductType;
import com.shinhan.esg_be.domain.bank.entity.enums.SavingStatus;
import com.shinhan.esg_be.domain.bank.repository.SavingHistoryRepository;
import com.shinhan.esg_be.domain.bank.repository.UserSavingRepository;
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
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SavingPaymentBatchServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-04-10T00:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    @InjectMocks
    private SavingPaymentBatchService savingPaymentBatchService;

    @Mock
    private UserSavingRepository userSavingRepository;

    @Mock
    private SavingHistoryRepository savingHistoryRepository;

    @Mock
    private Clock clock;

    @Test
    @DisplayName("자동 적금 납입은 납입일이 도래한 적금에 월 납입 이력을 생성한다")
    void processDuePaymentsCreatesMonthlySavingHistory() {
        UserSaving userSaving = createUserSaving(1L, LocalDate.of(2027, 3, 10), LocalDateTime.of(2026, 3, 10, 0, 0));

        given(clock.getZone()).willReturn(FIXED_CLOCK.getZone());
        given(clock.instant()).willReturn(FIXED_CLOCK.instant());
        given(userSavingRepository.findByStatus(SavingStatus.ACTIVE)).willReturn(List.of(userSaving));
        given(savingHistoryRepository.countByUserSaving_SavingId(1L)).willReturn(0L, 1L);

        savingPaymentBatchService.processDuePayments();

        ArgumentCaptor<SavingHistory> captor = ArgumentCaptor.forClass(SavingHistory.class);
        verify(savingHistoryRepository).save(captor.capture());
        SavingHistory savedHistory = captor.getValue();

        assertThat(savedHistory.getAmount()).isEqualTo(300_000L);
        assertThat(savedHistory.getPaymentDate()).isEqualTo(LocalDateTime.of(2026, 4, 10, 9, 0));
        assertThat(userSaving.getStatus()).isEqualTo(SavingStatus.ACTIVE);
    }

    @Test
    @DisplayName("자동 적금 납입은 마지막 회차 또는 만기 도달 시 적금을 종료한다")
    void processDuePaymentsCompletesSavingAtMaturity() {
        UserSaving userSaving = createUserSaving(1L, LocalDate.of(2026, 4, 10), LocalDateTime.of(2025, 4, 10, 0, 0));

        given(clock.getZone()).willReturn(FIXED_CLOCK.getZone());
        given(clock.instant()).willReturn(FIXED_CLOCK.instant());
        given(userSavingRepository.findByStatus(SavingStatus.ACTIVE)).willReturn(List.of(userSaving));
        given(savingHistoryRepository.countByUserSaving_SavingId(1L)).willReturn(11L, 12L);

        savingPaymentBatchService.processDuePayments();

        ArgumentCaptor<SavingHistory> captor = ArgumentCaptor.forClass(SavingHistory.class);
        verify(savingHistoryRepository).save(captor.capture());

        assertThat(captor.getValue().getAmount()).isEqualTo(300_000L);
        assertThat(userSaving.getStatus()).isEqualTo(SavingStatus.COMPLETE);
    }

    private UserSaving createUserSaving(Long savingId, LocalDate maturityDate, LocalDateTime joinedAt) {
        User user = newInstance(User.class);
        FinancialProduct product = newInstance(FinancialProduct.class);
        ReflectionTestUtils.setField(product, "type", ProductType.SAVINGS);

        UserSaving userSaving = newInstance(UserSaving.class);
        ReflectionTestUtils.setField(userSaving, "savingId", savingId);
        ReflectionTestUtils.setField(userSaving, "user", user);
        ReflectionTestUtils.setField(userSaving, "financialProduct", product);
        ReflectionTestUtils.setField(userSaving, "monthlyAmount", 300_000L);
        ReflectionTestUtils.setField(userSaving, "maturityDate", maturityDate);
        ReflectionTestUtils.setField(userSaving, "status", SavingStatus.ACTIVE);
        ReflectionTestUtils.setField(userSaving, "hasPenalty", false);
        ReflectionTestUtils.setField(userSaving, "score", 800);
        ReflectionTestUtils.setField(userSaving, "joinedAt", joinedAt);
        return userSaving;
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
