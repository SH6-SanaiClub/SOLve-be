package com.shinhan.esg_be.domain.bank.service;

import com.shinhan.esg_be.domain.bank.entity.LoanHistory;
import com.shinhan.esg_be.domain.bank.entity.UserLoan;
import com.shinhan.esg_be.domain.bank.entity.enums.LoanStatus;
import com.shinhan.esg_be.domain.bank.repository.LoanHistoryRepository;
import com.shinhan.esg_be.domain.bank.repository.UserLoanRepository;
import com.shinhan.esg_be.domain.reward.service.RewardService;
import com.shinhan.esg_be.domain.reward.service.command.ApplyActivityRewardCommand;
import com.shinhan.esg_be.global.common.enums.ActivityType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class LoanRepaymentBatchService {

    private static final int LOAN_DURATION_MONTHS = 12;

    private final UserLoanRepository userLoanRepository;
    private final LoanHistoryRepository loanHistoryRepository;
    private final RewardService rewardService;
    private final Clock clock;

    public void processDueRepayments() {
        LocalDate today = LocalDate.now(clock);
        LocalDateTime paidAt = LocalDateTime.now(clock);

        List<UserLoan> dueLoans = userLoanRepository.findByStatusAndNextRepaymentDateLessThanEqual(LoanStatus.ACTIVE, today);
        for (UserLoan userLoan : dueLoans) {
            processRepayment(userLoan, paidAt);
        }
    }

    void processRepayment(UserLoan userLoan, LocalDateTime paidAt) {
        long repaymentCount = loanHistoryRepository.countByUserLoan_LoanId(userLoan.getLoanId());
        long nextInstallment = repaymentCount + 1L;

        long paymentAmount = isFinalInstallment(userLoan, nextInstallment)
                ? calculateFinalRepaymentAmount(userLoan)
                : calculateMonthlyInterest(userLoan);

        loanHistoryRepository.save(LoanHistory.create(userLoan, paymentAmount, paidAt));
        rewardService.applyActivityReward(
                new ApplyActivityRewardCommand(
                        userLoan.getUser().getUserId(),
                        ActivityType.LOAN_REPAY,
                        null,
                        paidAt
                )
        );

        if (isFinalInstallment(userLoan, nextInstallment)) {
            userLoan.complete();
            return;
        }

        userLoan.advanceNextRepaymentDate();
    }

    private boolean isFinalInstallment(UserLoan userLoan, long installment) {
        LocalDate finalRepaymentDate = userLoan.getCreatedAt().toLocalDate().plusMonths(LOAN_DURATION_MONTHS);
        return installment >= LOAN_DURATION_MONTHS || !userLoan.getNextRepaymentDate().isBefore(finalRepaymentDate);
    }

    private long calculateMonthlyInterest(UserLoan userLoan) {
        return BigDecimal.valueOf(userLoan.getPrincipalAmount())
                .multiply(userLoan.getCurrentRate())
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(LOAN_DURATION_MONTHS), 0, RoundingMode.HALF_UP)
                .longValue();
    }

    private long calculateFinalRepaymentAmount(UserLoan userLoan) {
        return userLoan.getPrincipalAmount() + calculateMonthlyInterest(userLoan);
    }
}
