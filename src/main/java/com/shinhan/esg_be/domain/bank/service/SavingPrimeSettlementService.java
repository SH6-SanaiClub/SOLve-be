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

    private static final String EARTH_DEFENDER_KEYWORD = "\uC9C0\uAD6C \uC218\uD638\uB300";
    private static final String WARM_COMPANION_KEYWORD = "\uB530\uB73B\uD55C \uB3D9\uD589";
    private static final String SMART_FINANCE_KEYWORD = "\uBC14\uB978 \uAE08\uC735 \uC2A4\uB9C8\uD2B8";
    private static final String ESG_MASTER_KEYWORD = "ESG \uB9C8\uC2A4\uD130";
    private static final int ESG_MASTER_MIN_TOTAL_SCORE = 900;
    private static final BigDecimal EARTH_DEFENDER_MONTHLY_INCREMENT = new BigDecimal("0.20");
    private static final BigDecimal WARM_COMPANION_MONTHLY_INCREMENT = new BigDecimal("0.30");
    private static final BigDecimal SMART_FINANCE_MONTHLY_INCREMENT = new BigDecimal("0.10");

    private final UserSavingRepository userSavingRepository;
    private final SavingPrimeHistoryRepository savingPrimeHistoryRepository;
    private final UserMonthlyStatRepository userMonthlyStatRepository;
    private final EsgScorePolicyRepository esgScorePolicyRepository;
    private final Clock clock;

    public void settleMonthlyPrimeRates() {
        settleMonthlyPrimeRates(LocalDateTime.now(clock));
    }

    public void settleMonthlyPrimeRates(LocalDateTime settledAt) {
        EsgScorePolicy ePolicy = esgScorePolicyRepository.findByCategoryAndIsActiveTrue(ScoreCategory.E)
                .orElseThrow(() -> new IllegalArgumentException("ESG score policy not found."));
        EsgScorePolicy sPolicy = esgScorePolicyRepository.findByCategoryAndIsActiveTrue(ScoreCategory.S)
                .orElseThrow(() -> new IllegalArgumentException("ESG score policy not found."));
        EsgScorePolicy gPolicy = esgScorePolicyRepository.findByCategoryAndIsActiveTrue(ScoreCategory.G_ACTIVITY)
                .orElseThrow(() -> new IllegalArgumentException("ESG score policy not found."));

        int monthlyETarget = defaultIfNull(ePolicy.getMonthlyMaxScore());
        int monthlySTarget = defaultIfNull(sPolicy.getMonthlyMaxScore());
        int monthlyGTarget = defaultIfNull(gPolicy.getMonthlyMaxScore());

        List<UserSaving> activeSavings = userSavingRepository.findByStatus(SavingStatus.ACTIVE);
        for (UserSaving userSaving : activeSavings) {
            if (isEsgMasterSaving(userSaving)) {
                settleEsgMasterEligibility(userSaving);
                continue;
            }
            if (isEarthDefenderSaving(userSaving)) {
                settleOne(
                        userSaving,
                        monthlyETarget,
                        EARTH_DEFENDER_MONTHLY_INCREMENT,
                        getMonthlyEScore(userSaving),
                        settledAt
                );
                continue;
            }
            if (isWarmCompanionSaving(userSaving)) {
                settleOne(
                        userSaving,
                        monthlySTarget,
                        WARM_COMPANION_MONTHLY_INCREMENT,
                        getMonthlySScore(userSaving),
                        settledAt
                );
                continue;
            }
            if (isSmartFinanceSaving(userSaving)) {
                settleOne(
                        userSaving,
                        monthlyGTarget,
                        SMART_FINANCE_MONTHLY_INCREMENT,
                        getMonthlyGScore(userSaving),
                        settledAt
                );
            }
        }
    }

    private void settleOne(
            UserSaving userSaving,
            int monthlyTarget,
            BigDecimal monthlyIncrement,
            int monthlyScore,
            LocalDateTime settledAt
    ) {
        BigDecimal currentAddedRate = savingPrimeHistoryRepository
                .findTopByUserSaving_SavingIdOrderByAppliedAtDesc(userSaving.getSavingId())
                .map(SavingPrimeHistory::getAddedRate)
                .orElse(BigDecimal.ZERO);

        BigDecimal nextAddedRate = currentAddedRate;
        if (monthlyScore >= monthlyTarget && monthlyTarget > 0) {
            nextAddedRate = currentAddedRate.add(monthlyIncrement);
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

    private boolean isWarmCompanionSaving(UserSaving userSaving) {
        String productName = userSaving.getFinancialProduct().getName();
        return productName != null && productName.contains(WARM_COMPANION_KEYWORD);
    }

    private boolean isSmartFinanceSaving(UserSaving userSaving) {
        String productName = userSaving.getFinancialProduct().getName();
        return productName != null && productName.contains(SMART_FINANCE_KEYWORD);
    }

    private boolean isEsgMasterSaving(UserSaving userSaving) {
        String productName = userSaving.getFinancialProduct().getName();
        return productName != null && productName.contains(ESG_MASTER_KEYWORD);
    }

    private void settleEsgMasterEligibility(UserSaving userSaving) {
        if (userSaving.getUser().getTotalScore() < ESG_MASTER_MIN_TOTAL_SCORE) {
            userSaving.revokeMasterBonus();
        }
    }

    private int getMonthlyEScore(UserSaving userSaving) {
        return userMonthlyStatRepository.findByUser(userSaving.getUser())
                .map(UserMonthlyStat::getMonthlyEScore)
                .orElse(0);
    }

    private int getMonthlySScore(UserSaving userSaving) {
        return userMonthlyStatRepository.findByUser(userSaving.getUser())
                .map(UserMonthlyStat::getMonthlySScore)
                .orElse(0);
    }

    private int getMonthlyGScore(UserSaving userSaving) {
        return userMonthlyStatRepository.findByUser(userSaving.getUser())
                .map(UserMonthlyStat::getMonthlyGScore)
                .orElse(0);
    }

    private int defaultIfNull(Integer value) {
        return value == null ? 0 : value;
    }
}
