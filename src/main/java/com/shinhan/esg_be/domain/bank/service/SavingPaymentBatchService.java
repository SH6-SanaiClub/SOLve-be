package com.shinhan.esg_be.domain.bank.service;

import com.shinhan.esg_be.domain.bank.entity.SavingHistory;
import com.shinhan.esg_be.domain.bank.entity.SavingPrimeHistory;
import com.shinhan.esg_be.domain.bank.entity.UserSaving;
import com.shinhan.esg_be.domain.bank.entity.enums.SavingHistoryType;
import com.shinhan.esg_be.domain.bank.entity.enums.SavingStatus;
import com.shinhan.esg_be.domain.bank.repository.SavingHistoryRepository;
import com.shinhan.esg_be.domain.bank.repository.SavingPrimeHistoryRepository;
import com.shinhan.esg_be.domain.bank.repository.UserSavingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SavingPaymentBatchService {

    private static final int SAVING_DURATION_MONTHS = 12;
    private static final String GREEN_STEP_UP_NAME = "\uADF8\uB9B0 \uC2A4\uD15D\uC5C5 \uC801\uAE08";
    private static final String SMART_FINANCE_KEYWORD = "\uBC14\uB978 \uAE08\uC735 \uC2A4\uB9C8\uD2B8";
    private static final String ESG_MASTER_KEYWORD = "ESG \uB9C8\uC2A4\uD130";
    private static final int SCORE_STEP = 40;
    private static final BigDecimal STEP_RATE = new BigDecimal("1.00");
    private static final BigDecimal MAX_ADDED_RATE = new BigDecimal("2.40");
    private static final BigDecimal SMART_FINANCE_MONTHLY_INCREMENT = new BigDecimal("0.10");
    private static final BigDecimal SMART_FINANCE_MAINTAIN_BONUS = new BigDecimal("0.30");
    private static final BigDecimal SMART_FINANCE_NO_PENALTY_BONUS = new BigDecimal("1.00");
    private static final BigDecimal PERCENT = new BigDecimal("100");

    private final UserSavingRepository userSavingRepository;
    private final SavingHistoryRepository savingHistoryRepository;
    private final SavingPrimeHistoryRepository savingPrimeHistoryRepository;
    private final Clock clock;

    public void processDuePayments() {
        LocalDate today = LocalDate.now(clock);
        LocalDateTime paidAt = LocalDateTime.now(clock);

        List<UserSaving> activeSavings = userSavingRepository.findByStatus(SavingStatus.ACTIVE);
        for (UserSaving userSaving : activeSavings) {
            if (!isPaymentDue(userSaving, today)) {
                continue;
            }
            processPayment(userSaving, paidAt);
        }
    }

    void processPayment(UserSaving userSaving, LocalDateTime paidAt) {
        savingHistoryRepository.save(SavingHistory.create(
                userSaving,
                userSaving.getMonthlyAmount(),
                SavingHistoryType.PAYMENT,
                paidAt
        ));
        savePrimeRateIfGreenStepUp(userSaving, paidAt);

        long paymentCount = savingHistoryRepository.countBySavingIdAndType(
                userSaving.getSavingId(),
                SavingHistoryType.PAYMENT
        );
        if (isMatured(userSaving, paymentCount, paidAt.toLocalDate())) {
            saveMaturityPrimeRateIfSmartFinance(userSaving, paymentCount, paidAt);
            saveMaturityPrimeRateIfEsgMaster(userSaving, paymentCount, paidAt);
            savingHistoryRepository.save(SavingHistory.create(
                    userSaving,
                    calculateMaturityInterest(userSaving),
                    SavingHistoryType.INTEREST,
                    paidAt
            ));
            userSaving.complete();
        }
    }

    private boolean isPaymentDue(UserSaving userSaving, LocalDate today) {
        if (userSaving.getJoinedAt() == null) {
            return false;
        }

        long paymentCount = savingHistoryRepository.countBySavingIdAndType(
                userSaving.getSavingId(),
                SavingHistoryType.PAYMENT
        );
        LocalDate nextPaymentDate = userSaving.getJoinedAt()
                .toLocalDate()
                .plusMonths(paymentCount + 1L);

        return !nextPaymentDate.isAfter(today);
    }

    private boolean isMatured(UserSaving userSaving, long paymentCount, LocalDate today) {
        return paymentCount >= SAVING_DURATION_MONTHS || !today.isBefore(userSaving.getMaturityDate());
    }

    private long calculateMaturityInterest(UserSaving userSaving) {
        long paidAmount = savingHistoryRepository.sumAmountBySavingIdAndType(
                userSaving.getSavingId(),
                SavingHistoryType.PAYMENT
        );
        BigDecimal addedRate = savingPrimeHistoryRepository
                .findTopByUserSaving_SavingIdOrderByAppliedAtDesc(userSaving.getSavingId())
                .map(SavingPrimeHistory::getAddedRate)
                .orElse(BigDecimal.ZERO);
        BigDecimal appliedRate = userSaving.getFinancialProduct().getBaseRate()
                .add(addedRate)
                .min(userSaving.getFinancialProduct().getMaxRate());
        BigDecimal interest = BigDecimal.valueOf(paidAmount)
                .multiply(appliedRate)
                .divide(PERCENT, 0, RoundingMode.HALF_UP);
        return interest.longValue();
    }

    private void savePrimeRateIfGreenStepUp(UserSaving userSaving, LocalDateTime paidAt) {
        if (!GREEN_STEP_UP_NAME.equals(userSaving.getFinancialProduct().getName())) {
            return;
        }

        int currentScore = userSaving.getUser().getTotalScore();
        int baseScore = userSaving.getScore();
        int scoreDiff = Math.max(0, currentScore - baseScore);
        int stepCount = scoreDiff / SCORE_STEP;

        BigDecimal addedRate = STEP_RATE
                .multiply(BigDecimal.valueOf(stepCount))
                .setScale(2, RoundingMode.DOWN);
        if (addedRate.compareTo(MAX_ADDED_RATE) > 0) {
            addedRate = MAX_ADDED_RATE;
        }

        savingPrimeHistoryRepository.save(SavingPrimeHistory.create(userSaving, addedRate, paidAt));
    }

    private void saveMaturityPrimeRateIfSmartFinance(
            UserSaving userSaving,
            long paymentCount,
            LocalDateTime paidAt
    ) {
        if (!isSmartFinanceSaving(userSaving) || paymentCount < SAVING_DURATION_MONTHS) {
            return;
        }

        BigDecimal currentAddedRate = savingPrimeHistoryRepository
                .findTopByUserSaving_SavingIdOrderByAppliedAtDesc(userSaving.getSavingId())
                .map(SavingPrimeHistory::getAddedRate)
                .orElse(BigDecimal.ZERO);

        BigDecimal nextAddedRate = currentAddedRate;
        BigDecimal fullAchievementRate = SMART_FINANCE_MONTHLY_INCREMENT
                .multiply(BigDecimal.valueOf(SAVING_DURATION_MONTHS));
        if (currentAddedRate.compareTo(fullAchievementRate) >= 0) {
            nextAddedRate = nextAddedRate.add(SMART_FINANCE_MAINTAIN_BONUS);
        }
        if (Boolean.FALSE.equals(userSaving.getHasPenalty())) {
            nextAddedRate = nextAddedRate.add(SMART_FINANCE_NO_PENALTY_BONUS);
        }

        BigDecimal maxAddedRate = userSaving.getFinancialProduct().getMaxRate()
                .subtract(userSaving.getFinancialProduct().getBaseRate());
        if (nextAddedRate.compareTo(maxAddedRate) > 0) {
            nextAddedRate = maxAddedRate;
        }

        if (nextAddedRate.compareTo(currentAddedRate) > 0) {
            savingPrimeHistoryRepository.save(SavingPrimeHistory.create(userSaving, nextAddedRate, paidAt));
        }
    }

    private boolean isSmartFinanceSaving(UserSaving userSaving) {
        String productName = userSaving.getFinancialProduct().getName();
        return productName != null && productName.contains(SMART_FINANCE_KEYWORD);
    }

    private void saveMaturityPrimeRateIfEsgMaster(
            UserSaving userSaving,
            long paymentCount,
            LocalDateTime paidAt
    ) {
        if (!isEsgMasterSaving(userSaving) || paymentCount < SAVING_DURATION_MONTHS) {
            return;
        }
        if (Boolean.FALSE.equals(userSaving.getMasterBonusEligible())) {
            return;
        }

        BigDecimal maxAddedRate = userSaving.getFinancialProduct().getMaxRate()
                .subtract(userSaving.getFinancialProduct().getBaseRate());
        savingPrimeHistoryRepository.save(SavingPrimeHistory.create(userSaving, maxAddedRate, paidAt));
    }

    private boolean isEsgMasterSaving(UserSaving userSaving) {
        String productName = userSaving.getFinancialProduct().getName();
        return productName != null && productName.contains(ESG_MASTER_KEYWORD);
    }
}
