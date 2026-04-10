package com.shinhan.esg_be.domain.bank.service;

import com.shinhan.esg_be.domain.bank.entity.SavingHistory;
import com.shinhan.esg_be.domain.bank.entity.SavingPrimeHistory;
import com.shinhan.esg_be.domain.bank.entity.UserSaving;
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
    private static final String GREEN_STEP_UP_NAME = "그린 스텝업 적금";
    private static final int SCORE_STEP = 40;
    private static final BigDecimal STEP_RATE = new BigDecimal("1.00");
    private static final BigDecimal MAX_ADDED_RATE = new BigDecimal("2.40");

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
        savingHistoryRepository.save(SavingHistory.create(userSaving, userSaving.getMonthlyAmount(), paidAt));
        savePrimeRateIfGreenStepUp(userSaving, paidAt);

        long paymentCount = savingHistoryRepository.countByUserSaving_SavingId(userSaving.getSavingId());
        if (isMatured(userSaving, paymentCount, paidAt.toLocalDate())) {
            userSaving.complete();
        }
    }

    private boolean isPaymentDue(UserSaving userSaving, LocalDate today) {
        if (userSaving.getJoinedAt() == null) {
            return false;
        }

        long paymentCount = savingHistoryRepository.countByUserSaving_SavingId(userSaving.getSavingId());
        LocalDate nextPaymentDate = userSaving.getJoinedAt()
                .toLocalDate()
                .plusMonths(paymentCount + 1L);

        return !nextPaymentDate.isAfter(today);
    }

    private boolean isMatured(UserSaving userSaving, long paymentCount, LocalDate today) {
        return paymentCount >= SAVING_DURATION_MONTHS || !today.isBefore(userSaving.getMaturityDate());
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
}
