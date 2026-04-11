package com.shinhan.esg_be.domain.bank.service;

import com.shinhan.esg_be.domain.bank.entity.FinancialProduct;
import com.shinhan.esg_be.domain.bank.entity.SavingHistory;
import com.shinhan.esg_be.domain.bank.entity.SavingPrimeHistory;
import com.shinhan.esg_be.domain.bank.entity.UserSaving;
import com.shinhan.esg_be.domain.bank.entity.enums.ProductType;
import com.shinhan.esg_be.domain.bank.entity.enums.SavingStatus;
import com.shinhan.esg_be.domain.bank.repository.SavingHistoryRepository;
import com.shinhan.esg_be.domain.bank.repository.SavingPrimeHistoryRepository;
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
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

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
    private SavingPrimeHistoryRepository savingPrimeHistoryRepository;

    @Mock
    private Clock clock;

    @Test
    @DisplayName("자동 적금 납입일이 도래하면 납입 이력을 생성한다")
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
    @DisplayName("자동 적금 납입 후 만기 도달 시 적금 상태를 완료로 변경한다")
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

    @Test
    @DisplayName("그린 스텝업 적금 자동 납입 시 우대금리 이력을 저장한다")
    void processDuePaymentsSavesPrimeHistoryForGreenStepUp() {
        UserSaving userSaving = createUserSaving(1L, LocalDate.of(2027, 3, 10), LocalDateTime.of(2026, 3, 10, 0, 0));
        ReflectionTestUtils.setField(userSaving.getFinancialProduct(), "name", "\uADF8\uB9B0 \uC2A4\uD15D\uC5C5 \uC801\uAE08");
        ReflectionTestUtils.setField(userSaving, "score", 800);
        ReflectionTestUtils.setField(userSaving.getUser(), "eScore", 200);
        ReflectionTestUtils.setField(userSaving.getUser(), "sScore", 300);
        ReflectionTestUtils.setField(userSaving.getUser(), "gActivityScore", 220);
        ReflectionTestUtils.setField(userSaving.getUser(), "gRepaymentScore", 200);

        given(clock.getZone()).willReturn(FIXED_CLOCK.getZone());
        given(clock.instant()).willReturn(FIXED_CLOCK.instant());
        given(userSavingRepository.findByStatus(SavingStatus.ACTIVE)).willReturn(List.of(userSaving));
        given(savingHistoryRepository.countByUserSaving_SavingId(1L)).willReturn(0L, 1L);

        savingPaymentBatchService.processDuePayments();

        ArgumentCaptor<SavingPrimeHistory> primeCaptor = ArgumentCaptor.forClass(SavingPrimeHistory.class);
        verify(savingPrimeHistoryRepository).save(primeCaptor.capture());
        SavingPrimeHistory savedPrime = primeCaptor.getValue();
        assertThat(savedPrime.getAddedRate()).isEqualByComparingTo(new BigDecimal("2.40"));
        assertThat(savedPrime.getAppliedAt()).isEqualTo(LocalDateTime.of(2026, 4, 10, 9, 0));
    }

    @Test
    @DisplayName("그린 스텝업이 아닌 적금은 납입 시 우대금리 이력을 만들지 않는다")
    void processDuePaymentsDoesNotSavePrimeHistoryForNonGreenStepUp() {
        UserSaving userSaving = createUserSaving(1L, LocalDate.of(2027, 3, 10), LocalDateTime.of(2026, 3, 10, 0, 0));
        ReflectionTestUtils.setField(userSaving.getFinancialProduct(), "name", "Other Saving");

        given(clock.getZone()).willReturn(FIXED_CLOCK.getZone());
        given(clock.instant()).willReturn(FIXED_CLOCK.instant());
        given(userSavingRepository.findByStatus(SavingStatus.ACTIVE)).willReturn(List.of(userSaving));
        given(savingHistoryRepository.countByUserSaving_SavingId(1L)).willReturn(0L, 1L);

        savingPaymentBatchService.processDuePayments();

        verifyNoInteractions(savingPrimeHistoryRepository);
    }

    @Test
    @DisplayName("그린 스텝업 적금 점수 상승이 40 미만이면 우대금리 0.00을 저장한다")
    void processDuePaymentsSavesZeroPrimeRateWhenScoreDiffBelowStep() {
        UserSaving userSaving = createUserSaving(1L, LocalDate.of(2027, 3, 10), LocalDateTime.of(2026, 3, 10, 0, 0));
        ReflectionTestUtils.setField(userSaving.getFinancialProduct(), "name", "\uADF8\uB9B0 \uC2A4\uD15D\uC5C5 \uC801\uAE08");
        ReflectionTestUtils.setField(userSaving, "score", 800);
        ReflectionTestUtils.setField(userSaving.getUser(), "eScore", 200);
        ReflectionTestUtils.setField(userSaving.getUser(), "sScore", 300);
        ReflectionTestUtils.setField(userSaving.getUser(), "gActivityScore", 130);
        ReflectionTestUtils.setField(userSaving.getUser(), "gRepaymentScore", 200);

        given(clock.getZone()).willReturn(FIXED_CLOCK.getZone());
        given(clock.instant()).willReturn(FIXED_CLOCK.instant());
        given(userSavingRepository.findByStatus(SavingStatus.ACTIVE)).willReturn(List.of(userSaving));
        given(savingHistoryRepository.countByUserSaving_SavingId(1L)).willReturn(0L, 1L);

        savingPaymentBatchService.processDuePayments();

        ArgumentCaptor<SavingPrimeHistory> primeCaptor = ArgumentCaptor.forClass(SavingPrimeHistory.class);
        verify(savingPrimeHistoryRepository).save(primeCaptor.capture());
        SavingPrimeHistory savedPrime = primeCaptor.getValue();
        assertThat(savedPrime.getAddedRate()).isEqualByComparingTo(new BigDecimal("0.00"));
    }

    @Test
    @DisplayName("바른 금융 스마트 적금 만기 시 유지/무패널티 보너스를 반영한다")
    void processDuePaymentsAppliesMaturityBonusForSmartFinance() {
        UserSaving userSaving = createUserSaving(1L, LocalDate.of(2026, 4, 10), LocalDateTime.of(2025, 4, 10, 0, 0));
        ReflectionTestUtils.setField(userSaving.getFinancialProduct(), "name", "\uBC14\uB978 \uAE08\uC735 \uC2A4\uB9C8\uD2B8 \uC801\uAE08");
        ReflectionTestUtils.setField(userSaving.getFinancialProduct(), "baseRate", new BigDecimal("3.00"));
        ReflectionTestUtils.setField(userSaving.getFinancialProduct(), "maxRate", new BigDecimal("5.50"));
        ReflectionTestUtils.setField(userSaving, "hasPenalty", false);

        given(clock.getZone()).willReturn(FIXED_CLOCK.getZone());
        given(clock.instant()).willReturn(FIXED_CLOCK.instant());
        given(userSavingRepository.findByStatus(SavingStatus.ACTIVE)).willReturn(List.of(userSaving));
        given(savingHistoryRepository.countByUserSaving_SavingId(1L)).willReturn(11L, 12L);
        given(savingPrimeHistoryRepository.findTopByUserSaving_SavingIdOrderByAppliedAtDesc(1L))
                .willReturn(Optional.of(SavingPrimeHistory.create(
                        userSaving,
                        new BigDecimal("1.20"),
                        LocalDateTime.of(2026, 3, 10, 9, 0)
                )));

        savingPaymentBatchService.processDuePayments();

        ArgumentCaptor<SavingPrimeHistory> primeCaptor = ArgumentCaptor.forClass(SavingPrimeHistory.class);
        verify(savingPrimeHistoryRepository).save(primeCaptor.capture());
        SavingPrimeHistory savedPrime = primeCaptor.getValue();
        assertThat(savedPrime.getAddedRate()).isEqualByComparingTo(new BigDecimal("2.50"));
    }

    @Test
    @DisplayName("바른 금융 스마트 적금 만기라도 보너스 미충족이면 추가 저장하지 않는다")
    void processDuePaymentsSkipsMaturityBonusWhenNotEligible() {
        UserSaving userSaving = createUserSaving(1L, LocalDate.of(2026, 4, 10), LocalDateTime.of(2025, 4, 10, 0, 0));
        ReflectionTestUtils.setField(userSaving.getFinancialProduct(), "name", "\uBC14\uB978 \uAE08\uC735 \uC2A4\uB9C8\uD2B8 \uC801\uAE08");
        ReflectionTestUtils.setField(userSaving.getFinancialProduct(), "baseRate", new BigDecimal("3.00"));
        ReflectionTestUtils.setField(userSaving.getFinancialProduct(), "maxRate", new BigDecimal("5.50"));
        ReflectionTestUtils.setField(userSaving, "hasPenalty", true);

        given(clock.getZone()).willReturn(FIXED_CLOCK.getZone());
        given(clock.instant()).willReturn(FIXED_CLOCK.instant());
        given(userSavingRepository.findByStatus(SavingStatus.ACTIVE)).willReturn(List.of(userSaving));
        given(savingHistoryRepository.countByUserSaving_SavingId(1L)).willReturn(11L, 12L);
        given(savingPrimeHistoryRepository.findTopByUserSaving_SavingIdOrderByAppliedAtDesc(1L))
                .willReturn(Optional.of(SavingPrimeHistory.create(
                        userSaving,
                        new BigDecimal("1.10"),
                        LocalDateTime.of(2026, 3, 10, 9, 0)
                )));

        savingPaymentBatchService.processDuePayments();

        verify(savingPrimeHistoryRepository, never()).save(any(SavingPrimeHistory.class));
    }

    @Test
    @DisplayName("ESG 마스터 적금은 만기 시 자격 유지면 최대 우대금리를 적용한다")
    void processDuePaymentsAppliesMaturityPrimeForEsgMasterWhenEligible() {
        UserSaving userSaving = createUserSaving(1L, LocalDate.of(2026, 4, 10), LocalDateTime.of(2025, 4, 10, 0, 0));
        ReflectionTestUtils.setField(userSaving.getFinancialProduct(), "name", "ESG \uB9C8\uC2A4\uD130 \uC801\uAE08");
        ReflectionTestUtils.setField(userSaving.getFinancialProduct(), "baseRate", new BigDecimal("4.00"));
        ReflectionTestUtils.setField(userSaving.getFinancialProduct(), "maxRate", new BigDecimal("10.00"));
        ReflectionTestUtils.setField(userSaving, "masterBonusEligible", true);

        given(clock.getZone()).willReturn(FIXED_CLOCK.getZone());
        given(clock.instant()).willReturn(FIXED_CLOCK.instant());
        given(userSavingRepository.findByStatus(SavingStatus.ACTIVE)).willReturn(List.of(userSaving));
        given(savingHistoryRepository.countByUserSaving_SavingId(1L)).willReturn(11L, 12L);

        savingPaymentBatchService.processDuePayments();

        ArgumentCaptor<SavingPrimeHistory> primeCaptor = ArgumentCaptor.forClass(SavingPrimeHistory.class);
        verify(savingPrimeHistoryRepository).save(primeCaptor.capture());
        assertThat(primeCaptor.getValue().getAddedRate()).isEqualByComparingTo(new BigDecimal("6.00"));
    }

    @Test
    @DisplayName("ESG 마스터 적금은 자격 소멸 상태면 만기 우대금리를 적용하지 않는다")
    void processDuePaymentsSkipsMaturityPrimeForEsgMasterWhenIneligible() {
        UserSaving userSaving = createUserSaving(1L, LocalDate.of(2026, 4, 10), LocalDateTime.of(2025, 4, 10, 0, 0));
        ReflectionTestUtils.setField(userSaving.getFinancialProduct(), "name", "ESG \uB9C8\uC2A4\uD130 \uC801\uAE08");
        ReflectionTestUtils.setField(userSaving.getFinancialProduct(), "baseRate", new BigDecimal("4.00"));
        ReflectionTestUtils.setField(userSaving.getFinancialProduct(), "maxRate", new BigDecimal("10.00"));
        ReflectionTestUtils.setField(userSaving, "masterBonusEligible", false);

        given(clock.getZone()).willReturn(FIXED_CLOCK.getZone());
        given(clock.instant()).willReturn(FIXED_CLOCK.instant());
        given(userSavingRepository.findByStatus(SavingStatus.ACTIVE)).willReturn(List.of(userSaving));
        given(savingHistoryRepository.countByUserSaving_SavingId(1L)).willReturn(11L, 12L);

        savingPaymentBatchService.processDuePayments();

        verify(savingPrimeHistoryRepository, never()).save(any(SavingPrimeHistory.class));
    }

    private UserSaving createUserSaving(Long savingId, LocalDate maturityDate, LocalDateTime joinedAt) {
        User user = newInstance(User.class);
        FinancialProduct product = newInstance(FinancialProduct.class);
        ReflectionTestUtils.setField(product, "type", ProductType.SAVINGS);
        ReflectionTestUtils.setField(product, "name", "Test Saving");
        ReflectionTestUtils.setField(product, "baseRate", new BigDecimal("2.00"));
        ReflectionTestUtils.setField(product, "maxRate", new BigDecimal("4.40"));

        UserSaving userSaving = newInstance(UserSaving.class);
        ReflectionTestUtils.setField(userSaving, "savingId", savingId);
        ReflectionTestUtils.setField(userSaving, "user", user);
        ReflectionTestUtils.setField(userSaving, "financialProduct", product);
        ReflectionTestUtils.setField(userSaving, "monthlyAmount", 300_000L);
        ReflectionTestUtils.setField(userSaving, "maturityDate", maturityDate);
        ReflectionTestUtils.setField(userSaving, "status", SavingStatus.ACTIVE);
        ReflectionTestUtils.setField(userSaving, "hasPenalty", false);
        ReflectionTestUtils.setField(userSaving, "masterBonusEligible", true);
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
