package com.shinhan.esg_be.domain.bank.service;

import com.shinhan.esg_be.domain.bank.dto.response.FinanceProductListResponse;
import com.shinhan.esg_be.domain.bank.dto.response.FinanceProductResponse;
import com.shinhan.esg_be.domain.bank.entity.FinancialProduct;
import com.shinhan.esg_be.domain.bank.entity.enums.LoanStatus;
import com.shinhan.esg_be.domain.bank.entity.enums.ProductType;
import com.shinhan.esg_be.domain.bank.repository.FinancialProductRepository;
import com.shinhan.esg_be.domain.bank.repository.UserLoanRepository;
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
    private final UserLoanRepository userLoanRepository;
    private final UserRepository userRepository;

    public FinanceProductListResponse getFinanceProducts(String loginId, String type) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

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

    private record LoanOffer(
            boolean available,
            Long loanLimit,
            BigDecimal appliedRate
    ) {
    }
}
