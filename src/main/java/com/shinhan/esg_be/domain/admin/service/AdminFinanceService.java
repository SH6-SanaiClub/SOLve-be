package com.shinhan.esg_be.domain.admin.service;

import com.shinhan.esg_be.domain.admin.dto.request.AdminFinancialProductCreateRequest;
import com.shinhan.esg_be.domain.admin.dto.request.AdminFinancialProductUpdateRequest;
import com.shinhan.esg_be.domain.admin.dto.request.AdminStatusUpdateRequest;
import com.shinhan.esg_be.domain.admin.dto.response.AdminFinancialProductResponse;
import com.shinhan.esg_be.domain.admin.dto.response.AdminFinancialProductSubscriptionResponse;
import com.shinhan.esg_be.domain.bank.entity.FinancialProduct;
import com.shinhan.esg_be.domain.bank.entity.SavingPrimeHistory;
import com.shinhan.esg_be.domain.bank.entity.UserLoan;
import com.shinhan.esg_be.domain.bank.entity.UserSaving;
import com.shinhan.esg_be.domain.bank.entity.enums.LoanStatus;
import com.shinhan.esg_be.domain.bank.entity.enums.ProductType;
import com.shinhan.esg_be.domain.bank.entity.enums.SavingStatus;
import com.shinhan.esg_be.domain.bank.repository.FinancialProductRepository;
import com.shinhan.esg_be.domain.bank.repository.SavingPrimeHistoryRepository;
import com.shinhan.esg_be.domain.bank.repository.UserLoanRepository;
import com.shinhan.esg_be.domain.bank.repository.UserSavingRepository;
import com.shinhan.esg_be.global.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminFinanceService {

    private final FinancialProductRepository financialProductRepository;
    private final UserLoanRepository userLoanRepository;
    private final UserSavingRepository userSavingRepository;
    private final SavingPrimeHistoryRepository savingPrimeHistoryRepository;

    public List<AdminFinancialProductResponse> getFinancialProducts() {
        return financialProductRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(this::toProductResponse)
                .toList();
    }

    public List<AdminFinancialProductSubscriptionResponse> getSubscriptions(Long finProductId) {
        getFinancialProductEntity(finProductId);

        List<AdminFinancialProductSubscriptionResponse> subscriptions = new ArrayList<>();
        userLoanRepository.findAllByFinancialProduct_FinProductId(finProductId)
                .forEach(userLoan -> subscriptions.add(toLoanSubscriptionResponse(userLoan)));
        userSavingRepository.findAllByFinancialProduct_FinProductId(finProductId)
                .forEach(userSaving -> subscriptions.add(toSavingSubscriptionResponse(userSaving)));

        return subscriptions.stream()
                .sorted(Comparator.comparing(AdminFinancialProductSubscriptionResponse::getStartDate).reversed())
                .toList();
    }

    @Transactional
    public AdminFinancialProductResponse createFinancialProduct(AdminFinancialProductCreateRequest request) {
        validateFinancialProductRequest(request.getType(), request.getBaseRate(), request.getMaxRate(), request.getMonthlyPaymentAmount());

        FinancialProduct financialProduct = financialProductRepository.save(
                FinancialProduct.create(
                        request.getName(),
                        request.getSubtitle(),
                        request.getType(),
                        request.getBaseRate(),
                        request.getMaxRate(),
                        request.getDescription(),
                        request.getDurationMonths(),
                        request.getMonthlyPaymentAmount(),
                        request.getIsActive()
                )
        );

        return toProductResponse(financialProduct);
    }

    @Transactional
    public AdminFinancialProductResponse updateFinancialProduct(Long finProductId, AdminFinancialProductUpdateRequest request) {
        FinancialProduct financialProduct = getFinancialProductEntity(finProductId);
        validateFinancialProductRequest(request.getType(), request.getBaseRate(), request.getMaxRate(), request.getMonthlyPaymentAmount());
        validateProductTypeChange(financialProduct, request.getType());

        financialProduct.update(
                request.getName(),
                request.getSubtitle(),
                request.getType(),
                request.getBaseRate(),
                request.getMaxRate(),
                request.getDescription(),
                request.getDurationMonths(),
                request.getMonthlyPaymentAmount()
        );

        return toProductResponse(financialProduct);
    }

    @Transactional
    public AdminFinancialProductResponse updateFinancialProductStatus(Long finProductId, AdminStatusUpdateRequest request) {
        FinancialProduct financialProduct = getFinancialProductEntity(finProductId);
        financialProduct.updateStatus(request.getIsActive());
        return toProductResponse(financialProduct);
    }

    private AdminFinancialProductResponse toProductResponse(FinancialProduct financialProduct) {
        long activeLoanCount = userLoanRepository.countByFinancialProduct_FinProductIdAndStatus(
                financialProduct.getFinProductId(),
                LoanStatus.ACTIVE
        );
        long activeSavingCount = userSavingRepository.countByFinancialProduct_FinProductIdAndStatus(
                financialProduct.getFinProductId(),
                SavingStatus.ACTIVE
        );
        return AdminFinancialProductResponse.from(financialProduct, activeLoanCount, activeSavingCount);
    }

    private AdminFinancialProductSubscriptionResponse toLoanSubscriptionResponse(UserLoan userLoan) {
        return new AdminFinancialProductSubscriptionResponse(
                userLoan.getUser().getUserId(),
                userLoan.getUser().getLoginId(),
                userLoan.getUser().getName(),
                ProductType.LOAN.name(),
                userLoan.getPrincipalAmount(),
                userLoan.getCurrentRate(),
                userLoan.getStatus().name(),
                userLoan.getCreatedAt(),
                null,
                userLoan.getNextRepaymentDate()
        );
    }

    private AdminFinancialProductSubscriptionResponse toSavingSubscriptionResponse(UserSaving userSaving) {
        BigDecimal addedRate = savingPrimeHistoryRepository
                .findTopByUserSaving_SavingIdOrderByAppliedAtDesc(userSaving.getSavingId())
                .map(SavingPrimeHistory::getAddedRate)
                .orElse(BigDecimal.ZERO)
                .setScale(2, RoundingMode.DOWN);

        BigDecimal currentRate = userSaving.getFinancialProduct().getBaseRate()
                .add(addedRate)
                .min(userSaving.getFinancialProduct().getMaxRate());

        return new AdminFinancialProductSubscriptionResponse(
                userSaving.getUser().getUserId(),
                userSaving.getUser().getLoginId(),
                userSaving.getUser().getName(),
                ProductType.SAVINGS.name(),
                userSaving.getMonthlyAmount(),
                currentRate,
                userSaving.getStatus().name(),
                userSaving.getJoinedAt(),
                userSaving.getMaturityDate(),
                null
        );
    }

    private void validateFinancialProductRequest(
            ProductType type,
            BigDecimal baseRate,
            BigDecimal maxRate,
            Long monthlyPaymentAmount
    ) {
        if (baseRate.compareTo(maxRate) > 0) {
            throw new BadRequestException("기본 금리는 최대 금리보다 클 수 없습니다.");
        }
        if (type == ProductType.SAVINGS && (monthlyPaymentAmount == null || monthlyPaymentAmount <= 0)) {
            throw new BadRequestException("적금 상품은 월 납입액이 필요합니다.");
        }
    }

    private void validateProductTypeChange(FinancialProduct financialProduct, ProductType requestedType) {
        if (financialProduct.getType() == requestedType) {
            return;
        }

        long totalSubscriptionCount = userLoanRepository.findAllByFinancialProduct_FinProductId(financialProduct.getFinProductId()).size()
                + userSavingRepository.findAllByFinancialProduct_FinProductId(financialProduct.getFinProductId()).size();
        if (totalSubscriptionCount > 0) {
            throw new BadRequestException("가입 이력이 있는 금융 상품의 유형은 변경할 수 없습니다.");
        }
    }

    private FinancialProduct getFinancialProductEntity(Long finProductId) {
        return financialProductRepository.findById(finProductId)
                .orElseThrow(() -> new BadRequestException("금융 상품을 찾을 수 없습니다."));
    }
}
