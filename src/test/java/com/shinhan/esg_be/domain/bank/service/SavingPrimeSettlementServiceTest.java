package com.shinhan.esg_be.domain.bank.service;

import com.shinhan.esg_be.domain.bank.entity.FinancialProduct;
import com.shinhan.esg_be.domain.bank.entity.SavingPrimeHistory;
import com.shinhan.esg_be.domain.bank.entity.UserSaving;
import com.shinhan.esg_be.domain.bank.entity.enums.SavingStatus;
import com.shinhan.esg_be.domain.bank.repository.SavingPrimeHistoryRepository;
import com.shinhan.esg_be.domain.bank.repository.UserSavingRepository;
import com.shinhan.esg_be.domain.policy.entity.EsgScorePolicy;
import com.shinhan.esg_be.domain.policy.repository.EsgScorePolicyRepository;
import com.shinhan.esg_be.domain.stat.entity.UserMonthlyStat;
import com.shinhan.esg_be.domain.stat.repository.UserMonthlyStatRepository;
import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.global.common.enums.ScoreCategory;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SavingPrimeSettlementServiceTest {

    @InjectMocks
    private SavingPrimeSettlementService savingPrimeSettlementService;

    @Mock
    private UserSavingRepository userSavingRepository;

    @Mock
    private SavingPrimeHistoryRepository savingPrimeHistoryRepository;

    @Mock
    private UserMonthlyStatRepository userMonthlyStatRepository;

    @Mock
    private EsgScorePolicyRepository esgScorePolicyRepository;

    @Mock
    private Clock clock;

    @Test
    @DisplayName("따뜻한 동행 적금은 월 S 목표 달성 시 0.30%p 우대금리가 상한 내에서 누적된다")
    void settleMonthlyPrimeRatesForWarmCompanion() {
        LocalDateTime settledAt = LocalDateTime.of(2026, 5, 1, 0, 1);
        UserSaving userSaving = createUserSaving(1L, "\uB530\uB73B\uD55C \uB3D9\uD589 \uC801\uAE08", "3.00", "6.60");
        UserMonthlyStat monthlyStat = UserMonthlyStat.create(userSaving.getUser());
        ReflectionTestUtils.setField(monthlyStat, "monthlySScore", 40);

        given(esgScorePolicyRepository.findByCategoryAndIsActiveTrue(ScoreCategory.E))
                .willReturn(Optional.of(createPolicy(ScoreCategory.E, 25)));
        given(esgScorePolicyRepository.findByCategoryAndIsActiveTrue(ScoreCategory.S))
                .willReturn(Optional.of(createPolicy(ScoreCategory.S, 25)));
        given(userSavingRepository.findByStatus(SavingStatus.ACTIVE)).willReturn(List.of(userSaving));
        given(userMonthlyStatRepository.findByUser(userSaving.getUser())).willReturn(Optional.of(monthlyStat));
        given(savingPrimeHistoryRepository.findTopByUserSaving_SavingIdOrderByAppliedAtDesc(1L))
                .willReturn(Optional.of(SavingPrimeHistory.create(userSaving, new BigDecimal("3.50"), settledAt.minusMonths(1))));

        savingPrimeSettlementService.settleMonthlyPrimeRates(settledAt);

        ArgumentCaptor<SavingPrimeHistory> captor = ArgumentCaptor.forClass(SavingPrimeHistory.class);
        verify(savingPrimeHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getAddedRate()).isEqualByComparingTo("3.60");
        assertThat(captor.getValue().getAppliedAt()).isEqualTo(settledAt);
    }

    @Test
    @DisplayName("대상 적금이 아니면 우대금리 이력을 생성하지 않는다")
    void settleMonthlyPrimeRatesSkipNonTargetSaving() {
        LocalDateTime settledAt = LocalDateTime.of(2026, 5, 1, 0, 1);
        UserSaving userSaving = createUserSaving(2L, "General Saving", "2.00", "4.40");

        given(esgScorePolicyRepository.findByCategoryAndIsActiveTrue(ScoreCategory.E))
                .willReturn(Optional.of(createPolicy(ScoreCategory.E, 25)));
        given(esgScorePolicyRepository.findByCategoryAndIsActiveTrue(ScoreCategory.S))
                .willReturn(Optional.of(createPolicy(ScoreCategory.S, 25)));
        given(userSavingRepository.findByStatus(SavingStatus.ACTIVE)).willReturn(List.of(userSaving));

        savingPrimeSettlementService.settleMonthlyPrimeRates(settledAt);

        verifyNoInteractions(savingPrimeHistoryRepository, userMonthlyStatRepository);
    }

    private UserSaving createUserSaving(Long savingId, String name, String baseRate, String maxRate) {
        User user = newInstance(User.class);
        FinancialProduct product = newInstance(FinancialProduct.class);
        ReflectionTestUtils.setField(product, "name", name);
        ReflectionTestUtils.setField(product, "baseRate", new BigDecimal(baseRate));
        ReflectionTestUtils.setField(product, "maxRate", new BigDecimal(maxRate));

        UserSaving userSaving = newInstance(UserSaving.class);
        ReflectionTestUtils.setField(userSaving, "savingId", savingId);
        ReflectionTestUtils.setField(userSaving, "user", user);
        ReflectionTestUtils.setField(userSaving, "financialProduct", product);
        ReflectionTestUtils.setField(userSaving, "status", SavingStatus.ACTIVE);
        return userSaving;
    }

    private EsgScorePolicy createPolicy(ScoreCategory category, int monthlyMaxScore) {
        EsgScorePolicy policy = newInstance(EsgScorePolicy.class);
        ReflectionTestUtils.setField(policy, "category", category);
        ReflectionTestUtils.setField(policy, "monthlyMaxScore", monthlyMaxScore);
        ReflectionTestUtils.setField(policy, "isActive", true);
        return policy;
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
