package com.shinhan.esg_be.domain.bank.service;

import com.shinhan.esg_be.domain.bank.entity.SavingPrimeHistory;
import com.shinhan.esg_be.domain.bank.entity.UserSaving;
import com.shinhan.esg_be.domain.bank.entity.enums.SavingStatus;
import com.shinhan.esg_be.domain.bank.repository.SavingPrimeHistoryRepository;
import com.shinhan.esg_be.domain.bank.repository.UserSavingRepository;
import com.shinhan.esg_be.domain.policy.entity.EsgScorePolicy;
import com.shinhan.esg_be.domain.policy.repository.EsgScorePolicyRepository;
import com.shinhan.esg_be.domain.stat.entity.UserMonthlyStat;
import com.shinhan.esg_be.domain.stat.repository.UserMonthlyStatRepository;
import com.shinhan.esg_be.global.common.enums.ScoreCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SavingPrimeSettlementService {

    private static final String EARTH_DEFENDER_KEYWORD = "지구 수호대";
    private static final BigDecimal EARTH_DEFENDER_MONTHLY_INCREMENT = new BigDecimal("0.20");

    private final UserSavingRepository userSavingRepository;
    private final SavingPrimeHistoryRepository savingPrimeHistoryRepository;
    private final UserMonthlyStatRepository userMonthlyStatRepository;
    private final EsgScorePolicyRepository esgScorePolicyRepository;
    private final Clock clock;

    public void settleEarthDefenderPrime() {
        settleEarthDefenderPrime(LocalDateTime.now(clock));
    }

    public void settleEarthDefenderPrime(LocalDateTime settledAt) {
        EsgScorePolicy ePolicy = esgScorePolicyRepository.findByCategoryAndIsActiveTrue(ScoreCategory.E)
                .orElseThrow(() -> new IllegalArgumentException("ESG score policy not found."));
        int monthlyTarget = defaultIfNull(ePolicy.getMonthlyMaxScore());

        List<UserSaving> activeSavings = userSavingRepository.findByStatus(SavingStatus.ACTIVE);
        for (UserSaving userSaving : activeSavings) {
            if (!isEarthDefenderSaving(userSaving)) {
                continue;
            }
            settleOne(userSaving, monthlyTarget, settledAt);
        }
    }

    private void settleOne(UserSaving userSaving, int monthlyTarget, LocalDateTime settledAt) {
        BigDecimal currentAddedRate = savingPrimeHistoryRepository
                .findTopByUserSaving_SavingIdOrderByAppliedAtDesc(userSaving.getSavingId())
                .map(SavingPrimeHistory::getAddedRate)
                .orElse(BigDecimal.ZERO);

        int monthlyEScore = userMonthlyStatRepository.findByUser(userSaving.getUser())
                .map(UserMonthlyStat::getMonthlyEScore)
                .orElse(0);

        BigDecimal nextAddedRate = currentAddedRate;
        if (monthlyEScore >= monthlyTarget && monthlyTarget > 0) {
            nextAddedRate = currentAddedRate.add(EARTH_DEFENDER_MONTHLY_INCREMENT);
        }

        BigDecimal maxAddedRate = userSaving.getFinancialProduct().getMaxRate()
                .subtract(userSaving.getFinancialProduct().getBaseRate());
        if (nextAddedRate.compareTo(maxAddedRate) > 0) {
            nextAddedRate = maxAddedRate;
        }

        savingPrimeHistoryRepository.save(SavingPrimeHistory.create(userSaving, nextAddedRate, settledAt));
    }

    private boolean isEarthDefenderSaving(UserSaving userSaving) {
        String productName = userSaving.getFinancialProduct().getName();
        return productName != null && productName.contains(EARTH_DEFENDER_KEYWORD);
    }

    private int defaultIfNull(Integer value) {
        return value == null ? 0 : value;
    }
}
