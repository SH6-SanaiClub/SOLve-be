package com.shinhan.esg_be.domain.bank.service;

import com.shinhan.esg_be.domain.bank.dto.response.ActiveLoanResponse;
import com.shinhan.esg_be.domain.bank.dto.response.ActiveSavingResponse;
import com.shinhan.esg_be.domain.bank.dto.response.FinanceHistoryResponse;
import com.shinhan.esg_be.domain.bank.dto.response.FinanceMyResponse;
import com.shinhan.esg_be.domain.bank.dto.response.FinanceProductListResponse;
import com.shinhan.esg_be.domain.bank.dto.response.FinanceProductResponse;
import com.shinhan.esg_be.domain.bank.dto.response.LoanHistoryResponse;
import com.shinhan.esg_be.domain.bank.dto.response.SavingHistoryResponse;
import com.shinhan.esg_be.domain.bank.entity.FinancialProduct;
import com.shinhan.esg_be.domain.bank.entity.LoanHistory;
import com.shinhan.esg_be.domain.bank.entity.SavingHistory;
import com.shinhan.esg_be.domain.bank.entity.UserLoan;
import com.shinhan.esg_be.domain.bank.entity.UserSaving;
import com.shinhan.esg_be.domain.bank.entity.enums.LoanStatus;
import com.shinhan.esg_be.domain.bank.entity.enums.ProductType;
import com.shinhan.esg_be.domain.bank.entity.enums.SavingStatus;
import com.shinhan.esg_be.domain.bank.repository.FinancialProductRepository;
import com.shinhan.esg_be.domain.bank.repository.LoanHistoryRepository;
import com.shinhan.esg_be.domain.bank.repository.SavingHistoryRepository;
import com.shinhan.esg_be.domain.bank.repository.UserLoanRepository;
import com.shinhan.esg_be.domain.bank.repository.UserSavingRepository;
import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.repository.UserRepository;
import com.shinhan.esg_be.global.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinanceService {

    private static final long LOAN_LIMIT_700 = 1_000_000L;
    private static final long LOAN_LIMIT_800 = 2_000_000L;
    private static final long LOAN_LIMIT_900 = 3_000_000L;

    private static final BigDecimal LOAN_RATE_700 = new BigDecimal("8.50");
    private static final BigDecimal LOAN_RATE_800 = new BigDecimal("7.00");
    private static final BigDecimal LOAN_RATE_900 = new BigDecimal("6.00");

    private final FinancialProductRepository financialProductRepository;
    private final LoanHistoryRepository loanHistoryRepository;
    private final SavingHistoryRepository savingHistoryRepository;
    private final UserLoanRepository userLoanRepository;
    private final UserSavingRepository userSavingRepository;
    private final UserRepository userRepository;

    public FinanceMyResponse getMyFinance(String loginId) {
        User user = getUser(loginId);

        ActiveLoanResponse activeLoan = userLoanRepository.findByUser_UserIdAndStatus(user.getUserId(), LoanStatus.ACTIVE)
                .map(this::toActiveLoanResponse)
                .orElse(null);

        ActiveSavingResponse activeSaving = userSavingRepository.findByUser_UserIdAndStatus(user.getUserId(), SavingStatus.ACTIVE)
                .map(this::toActiveSavingResponse)
                .orElse(null);

        return new FinanceMyResponse(activeLoan, activeSaving);
    }

    public FinanceHistoryResponse getFinanceHistory(String loginId) {
        User user = getUser(loginId);

        List<LoanHistoryResponse> loans = loanHistoryRepository.findAllByUserId(user.getUserId())
                .stream()
                .map(this::toLoanHistoryResponse)
                .toList();

        List<SavingHistoryResponse> savings = savingHistoryRepository.findAllByUserId(user.getUserId())
                .stream()
                .map(this::toSavingHistoryResponse)
                .toList();

        return new FinanceHistoryResponse(loans, savings);
    }

    public FinanceProductListResponse getFinanceProducts(String loginId, String type) {
        User user = getUser(loginId);

        ProductType productType = parseProductType(type);
        var products = financialProductRepository.findByTypeAndIsActiveTrue(productType)
                .stream()
                .map(product -> toResponse(user, product, productType))
                .toList();

        return new FinanceProductListResponse(products);
    }

    private FinanceProductResponse toResponse(User user, FinancialProduct product, ProductType productType) {
        if (productType == ProductType.LOAN) {
            LoanOffer loanOffer = calculateLoanOffer(user);
            boolean hasActiveLoan = !userLoanRepository.findByUserAndStatus(user, LoanStatus.ACTIVE).isEmpty();
            boolean available = loanOffer.available() && !hasActiveLoan && !user.getIsLoanBlocked();

            return new FinanceProductResponse(
                    product.getFinProductId(),
                    product.getName(),
                    product.getSubtitle(),
                    product.getType().name(),
                    product.getBaseRate(),
                    product.getMaxRate(),
                    available ? loanOffer.appliedRate() : null,
                    available ? loanOffer.loanLimit() : null,
                    available,
                    product.getDurationMonths(),
                    product.getMonthlyPaymentAmount(),
                    product.getDescription()
            );
        }

        return new FinanceProductResponse(
                product.getFinProductId(),
                product.getName(),
                product.getSubtitle(),
                product.getType().name(),
                product.getBaseRate(),
                product.getMaxRate(),
                product.getBaseRate(),
                null,
                true,
                product.getDurationMonths(),
                product.getMonthlyPaymentAmount(),
                product.getDescription()
        );
    }

    private ActiveLoanResponse toActiveLoanResponse(UserLoan userLoan) {
        FinancialProduct product = userLoan.getFinancialProduct();
        long paidAmount = loanHistoryRepository.sumAmountByLoanId(userLoan.getLoanId());
        long remainingAmount = Math.max(userLoan.getTotalAmount() - paidAmount, 0L);
        long repaymentCount = loanHistoryRepository.countByUserLoan_LoanId(userLoan.getLoanId());

        return new ActiveLoanResponse(
                userLoan.getLoanId(),
                product.getFinProductId(),
                product.getName(),
                userLoan.getPrincipalAmount(),
                userLoan.getTotalAmount(),
                paidAmount,
                remainingAmount,
                repaymentCount,
                userLoan.getCurrentRate(),
                userLoan.getStatus().name(),
                product.getDurationMonths(),
                userLoan.getNextRepaymentDate(),
                userLoan.getCreatedAt()
        );
    }

    private ActiveSavingResponse toActiveSavingResponse(UserSaving userSaving) {
        FinancialProduct product = userSaving.getFinancialProduct();
        return new ActiveSavingResponse(
                userSaving.getSavingId(),
                product.getFinProductId(),
                product.getName(),
                userSaving.getMonthlyAmount(),
                userSaving.getStatus().name(),
                product.getDurationMonths(),
                userSaving.getHasPenalty(),
                userSaving.getMaturityDate(),
                userSaving.getJoinedAt()
        );
    }

    private LoanHistoryResponse toLoanHistoryResponse(LoanHistory loanHistory) {
        UserLoan userLoan = loanHistory.getUserLoan();
        FinancialProduct product = userLoan.getFinancialProduct();
        return new LoanHistoryResponse(
                loanHistory.getId(),
                userLoan.getLoanId(),
                product.getFinProductId(),
                product.getName(),
                loanHistory.getAmount(),
                loanHistory.getPaymentDate()
        );
    }

    private SavingHistoryResponse toSavingHistoryResponse(SavingHistory savingHistory) {
        UserSaving userSaving = savingHistory.getUserSaving();
        FinancialProduct product = userSaving.getFinancialProduct();
        return new SavingHistoryResponse(
                savingHistory.getId(),
                userSaving.getSavingId(),
                product.getFinProductId(),
                product.getName(),
                savingHistory.getAmount(),
                savingHistory.getPaymentDate()
        );
    }

    private LoanOffer calculateLoanOffer(User user) {
        int totalScore = user.getTotalScore();
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

    private ProductType parseProductType(String type) {
        if (type == null || type.isBlank()) {
            throw new BadRequestException("Product type is required.");
        }
        if ("loan".equalsIgnoreCase(type)) {
            return ProductType.LOAN;
        }
        if ("savings".equalsIgnoreCase(type)) {
            return ProductType.SAVINGS;
        }
        throw new BadRequestException("Invalid product type.");
    }

    private User getUser(String loginId) {
        return userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new BadRequestException("User not found."));
    }

    private record LoanOffer(
            boolean available,
            Long loanLimit,
            BigDecimal appliedRate
    ) {
    }
}
