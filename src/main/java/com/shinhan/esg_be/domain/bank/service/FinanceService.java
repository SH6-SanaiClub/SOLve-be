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
import com.shinhan.esg_be.domain.bank.entity.SavingPrimeHistory;
import com.shinhan.esg_be.domain.bank.entity.UserLoan;
import com.shinhan.esg_be.domain.bank.entity.UserSaving;
import com.shinhan.esg_be.domain.bank.entity.enums.LoanStatus;
import com.shinhan.esg_be.domain.bank.entity.enums.ProductType;
import com.shinhan.esg_be.domain.bank.entity.enums.SavingStatus;
import com.shinhan.esg_be.domain.bank.repository.FinancialProductRepository;
import com.shinhan.esg_be.domain.bank.repository.LoanHistoryRepository;
import com.shinhan.esg_be.domain.bank.repository.SavingHistoryRepository;
import com.shinhan.esg_be.domain.bank.repository.SavingPrimeHistoryRepository;
import com.shinhan.esg_be.domain.bank.repository.UserLoanRepository;
import com.shinhan.esg_be.domain.bank.repository.UserSavingRepository;
import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.repository.UserRepository;
import com.shinhan.esg_be.global.exception.BadRequestException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinanceService {

    private static final long SAVING_DURATION_MONTHS = 12L;

    private static final long LOAN_LIMIT_700 = 1_000_000L;
    private static final long LOAN_LIMIT_800 = 2_000_000L;
    private static final long LOAN_LIMIT_900 = 3_000_000L;

    private static final BigDecimal LOAN_RATE_700 = new BigDecimal("8.50");
    private static final BigDecimal LOAN_RATE_800 = new BigDecimal("7.00");
    private static final BigDecimal LOAN_RATE_900 = new BigDecimal("6.00");

    private final FinancialProductRepository financialProductRepository;
    private final LoanHistoryRepository loanHistoryRepository;
    private final SavingHistoryRepository savingHistoryRepository;
    private final SavingPrimeHistoryRepository savingPrimeHistoryRepository;
    private final UserLoanRepository userLoanRepository;
    private final UserSavingRepository userSavingRepository;
    private final UserRepository userRepository;
    
    @PersistenceContext
    private EntityManager entityManager;

    public FinanceMyResponse getMyFinance(String loginId) {
        User user = getUser(loginId);

        List<ActiveLoanResponse> loans = userLoanRepository
                .findAllByUser_UserIdAndStatus(user.getUserId(), LoanStatus.ACTIVE)
                .stream()
                .map(this::toActiveLoanResponse)
                .toList();

        List<ActiveSavingResponse> savings = userSavingRepository
                .findAllByUser_UserIdAndStatus(user.getUserId(), SavingStatus.ACTIVE)
                .stream()
                .map(this::toActiveSavingResponse)
                .toList();

        return new FinanceMyResponse(loans, savings);
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
        Set<Long> activeSavingProductIds = getActiveSavingProductIds(user, productType);
        var products = financialProductRepository.findByTypeAndIsActiveTrue(productType)
                .stream()
                .filter(product -> productType == ProductType.LOAN || !activeSavingProductIds.contains(product.getFinProductId()))
                .map(product -> toResponse(user, product, productType))
                .toList();

        return new FinanceProductListResponse(products);
    }

    private Set<Long> getActiveSavingProductIds(User user, ProductType productType) {
        if (productType == ProductType.LOAN) {
            return Set.of();
        }

        return userSavingRepository.findAllByUser_UserIdAndStatus(user.getUserId(), SavingStatus.ACTIVE)
                .stream()
                .map(userSaving -> userSaving.getFinancialProduct().getFinProductId())
                .collect(Collectors.toSet());
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
        BigDecimal addedRate = savingPrimeHistoryRepository
                .findTopByUserSaving_SavingIdOrderByAppliedAtDesc(userSaving.getSavingId())
                .map(SavingPrimeHistory::getAddedRate)
                .orElse(BigDecimal.ZERO)
                .setScale(2, java.math.RoundingMode.DOWN);
        BigDecimal appliedRate = product.getBaseRate()
                .add(addedRate)
                .min(product.getMaxRate());

        long paidAmount = savingHistoryRepository.sumAmountBySavingId(userSaving.getSavingId());
        long paymentCount = savingHistoryRepository.countByUserSaving_SavingId(userSaving.getSavingId());
        long remainingCount = Math.max(SAVING_DURATION_MONTHS - paymentCount, 0L);

        return new ActiveSavingResponse(
                userSaving.getSavingId(),
                product.getFinProductId(),
                product.getName(),
                userSaving.getMonthlyAmount(),
                addedRate,
                appliedRate,
                paidAmount,
                paymentCount,
                remainingCount,
                userSaving.getStatus().name(),
                product.getDurationMonths(),
                userSaving.getHasPenalty(),
                userSaving.getMasterBonusEligible(),
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
        try {
            return userRepository.findByLoginId(loginId)
                    .orElseThrow(() -> new BadRequestException("User not found."));
        } catch (IncorrectResultSizeDataAccessException ignored) {
            return entityManager.createQuery(
                            "select u from User u where u.loginId = :loginId order by u.userId desc",
                            User.class
                    )
                    .setParameter("loginId", loginId)
                    .setMaxResults(1)
                    .getResultList()
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException("User not found."));
        }
    }

    private record LoanOffer(
            boolean available,
            Long loanLimit,
            BigDecimal appliedRate
    ) {
    }
}
