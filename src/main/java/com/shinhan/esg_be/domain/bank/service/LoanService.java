package com.shinhan.esg_be.domain.bank.service;

import com.shinhan.esg_be.domain.bank.dto.request.LoanApplyRequest;
import com.shinhan.esg_be.domain.bank.dto.response.LoanApplyResponse;
import com.shinhan.esg_be.domain.bank.dto.response.LoanPreviewResponse;
import com.shinhan.esg_be.domain.bank.entity.FinancialProduct;
import com.shinhan.esg_be.domain.bank.entity.LoanHistory;
import com.shinhan.esg_be.domain.bank.entity.UserLoan;
import com.shinhan.esg_be.domain.bank.entity.enums.LoanStatus;
import com.shinhan.esg_be.domain.bank.entity.enums.ProductType;
import com.shinhan.esg_be.domain.bank.repository.FinancialProductRepository;
import com.shinhan.esg_be.domain.bank.repository.LoanHistoryRepository;
import com.shinhan.esg_be.domain.bank.repository.UserLoanRepository;
import com.shinhan.esg_be.domain.bank.service.screening.LoanScreeningPolicy;
import com.shinhan.esg_be.domain.bank.service.screening.LoanScreeningResult;
import com.shinhan.esg_be.domain.bank.service.screening.LoanScreeningService;
import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.repository.UserRepository;
import com.shinhan.esg_be.global.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class LoanService {

    private static final long LOAN_LIMIT_700 = 1_000_000L;
    private static final long LOAN_LIMIT_800 = 2_000_000L;
    private static final long LOAN_LIMIT_900 = 3_000_000L;

    private static final BigDecimal LOAN_RATE_700 = new BigDecimal("8.50");
    private static final BigDecimal LOAN_RATE_800 = new BigDecimal("7.00");
    private static final BigDecimal LOAN_RATE_900 = new BigDecimal("6.00");

    private final UserRepository userRepository;
    private final FinancialProductRepository financialProductRepository;
    private final UserLoanRepository userLoanRepository;
    private final LoanHistoryRepository loanHistoryRepository;
    private final LoanScreeningService loanScreeningService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public LoanPreviewResponse getLoanPreview(String loginId, Long productId) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new BadRequestException("사용자를 찾을 수 없습니다."));

        FinancialProduct product = financialProductRepository.findByFinProductIdAndTypeAndIsActiveTrue(
                        productId,
                        ProductType.LOAN
                )
                .orElseThrow(() -> new BadRequestException("대출 상품을 찾을 수 없습니다."));

        boolean hasActiveLoan = !userLoanRepository.findByUserAndStatus(user, LoanStatus.ACTIVE).isEmpty();
        boolean loanBlocked = user.getIsLoanBlocked();
        LoanScreeningResult screeningResult = loanScreeningService.screen(
                user.getUserId(),
                user.getTotalScore(),
                hasActiveLoan,
                loanBlocked
        );
        boolean available = screeningResult.approved();

        return new LoanPreviewResponse(
                product.getFinProductId(),
                product.getName(),
                product.getSubtitle(),
                product.getDescription(),
                available,
                toPreviewReason(screeningResult),
                available ? screeningResult.loanLimit() : null,
                available ? screeningResult.appliedRate() : null,
                product.getDurationMonths(),
                user.getTotalScore(),
                hasActiveLoan,
                loanBlocked
        );
    }

    public LoanApplyResponse applyLoan(String loginId, LoanApplyRequest request) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new BadRequestException("사용자를 찾을 수 없습니다."));

        FinancialProduct product = financialProductRepository.findByFinProductIdAndTypeAndIsActiveTrue(
                        request.loanId(),
                        ProductType.LOAN
                )
                .orElseThrow(() -> new BadRequestException("대출 상품을 찾을 수 없습니다."));

        if (user.getIsLoanBlocked()) {
            throw new BadRequestException("대출이 제한된 사용자입니다.");
        }

        if (userLoanRepository.existsByUserAndFinancialProductAndStatus(user, product, LoanStatus.ACTIVE)) {
            throw new BadRequestException("Already joined loan product.");
        }

        if (!userLoanRepository.findByUserAndStatus(user, LoanStatus.ACTIVE).isEmpty()) {
            throw new BadRequestException("기존 대출 상환 전까지 추가 대출이 불가능합니다.");
        }

        LoanScreeningResult screeningResult = loanScreeningService.screen(
                user.getUserId(),
                user.getTotalScore(),
                false,
                false
        );
        if (!screeningResult.approved()) {
            throw new BadRequestException("대출 신청 가능 점수를 충족하지 않습니다.");
        }

        if (request.amount() > screeningResult.loanLimit()) {
            throw new BadRequestException("대출 한도를 초과한 금액입니다.");
        }

        UserLoan userLoan = UserLoan.create(
                user,
                product,
                request.amount(),
                screeningResult.appliedRate(),
                calculateTotalAmount(request.amount(), screeningResult.appliedRate()),
                LocalDate.now(clock).plusMonths(1),
                user.getTotalScore()
        );

        userLoanRepository.save(userLoan);
        loanHistoryRepository.save(LoanHistory.create(userLoan, request.amount(), LocalDateTime.now(clock)));
        return new LoanApplyResponse(userLoan.getStatus().name());
    }

    private LoanOffer calculateLoanOffer(int totalScore) {
        if (totalScore >= 900) {
            return new LoanOffer(true, LOAN_LIMIT_900, LOAN_RATE_900);
        }
        if (totalScore >= 800) {
            return new LoanOffer(true, LOAN_LIMIT_800, LOAN_RATE_800);
        }
        if (totalScore >= 700) {
            return new LoanOffer(true, LOAN_LIMIT_700, LOAN_RATE_700);
        }
        return new LoanOffer(false, null, null);
    }

    private long calculateTotalAmount(long principalAmount, BigDecimal rate) {
        BigDecimal interest = BigDecimal.valueOf(principalAmount)
                .multiply(rate)
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        return principalAmount + interest.longValue();
    }

    private String toPreviewReason(LoanScreeningResult screeningResult) {
        if (screeningResult.approved()) {
            return "AVAILABLE";
        }

        if (LoanScreeningPolicy.REASON_LOAN_BLOCKED.equals(screeningResult.reason())) {
            return "LOAN_BLOCKED";
        }

        if (LoanScreeningPolicy.REASON_HAS_ACTIVE_LOAN.equals(screeningResult.reason())) {
            return "HAS_ACTIVE_LOAN";
        }

        return "LOW_SCORE";
    }

    private String determineUnavailableReason(boolean scoreAvailable, boolean hasActiveLoan, boolean loanBlocked) {
        if (loanBlocked) {
            return "LOAN_BLOCKED";
        }
        if (hasActiveLoan) {
            return "HAS_ACTIVE_LOAN";
        }
        if (!scoreAvailable) {
            return "LOW_SCORE";
        }
        return "AVAILABLE";
    }

    private record LoanOffer(
            boolean available,
            Long loanLimit,
            BigDecimal appliedRate
    ) {
    }
}
