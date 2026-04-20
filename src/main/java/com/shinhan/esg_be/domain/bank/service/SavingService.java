package com.shinhan.esg_be.domain.bank.service;

import com.shinhan.esg_be.domain.ai.service.ChatContextService;
import com.shinhan.esg_be.domain.bank.dto.request.SavingApplyRequest;
import com.shinhan.esg_be.domain.bank.dto.response.SavingApplyResponse;
import com.shinhan.esg_be.domain.bank.entity.FinancialProduct;
import com.shinhan.esg_be.domain.bank.entity.UserSaving;
import com.shinhan.esg_be.domain.bank.entity.enums.ProductType;
import com.shinhan.esg_be.domain.bank.entity.enums.SavingStatus;
import com.shinhan.esg_be.domain.bank.repository.FinancialProductRepository;
import com.shinhan.esg_be.domain.bank.repository.UserSavingRepository;
import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.repository.UserRepository;
import com.shinhan.esg_be.global.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional
public class SavingService {

    private static final long DEFAULT_MONTHLY_AMOUNT = 300_000L;
    private static final String ESG_MASTER_KEYWORD = "ESG 마스터";
    private static final int ESG_MASTER_MIN_TOTAL_SCORE = 900;

    private final UserRepository userRepository;
    private final FinancialProductRepository financialProductRepository;
    private final UserSavingRepository userSavingRepository;
    private final ChatContextService chatContextService;

    public SavingApplyResponse applySaving(String loginId, SavingApplyRequest request) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new BadRequestException("사용자를 찾을 수 없습니다."));

        FinancialProduct product = financialProductRepository.findByFinProductIdAndTypeAndIsActiveTrue(
                        request.productId(),
                        ProductType.SAVINGS
                )
                .orElseThrow(() -> new BadRequestException("적금 상품을 찾을 수 없습니다."));

        if (userSavingRepository.existsByUserAndFinancialProductAndStatus(user, product, SavingStatus.ACTIVE)) {
            throw new BadRequestException("Already joined saving product.");
        }

        if (isEsgMasterSaving(product) && user.getTotalScore() < ESG_MASTER_MIN_TOTAL_SCORE) {
            throw new BadRequestException("ESG 마스터 적금은 900점 이상부터 가입할 수 있습니다.");
        }

        Long monthlyAmount = product.getMonthlyPaymentAmount() != null
                ? product.getMonthlyPaymentAmount()
                : DEFAULT_MONTHLY_AMOUNT;

        UserSaving userSaving = UserSaving.create(
                user,
                product,
                monthlyAmount,
                LocalDate.now().plusMonths(product.getDurationMonths()),
                user.getTotalScore()
        );

        userSavingRepository.save(userSaving);
        chatContextService.evict(user.getUserId());
        return new SavingApplyResponse(userSaving.getStatus().name());
    }

    private boolean isEsgMasterSaving(FinancialProduct product) {
        String productName = product.getName();
        return productName != null && productName.contains(ESG_MASTER_KEYWORD);
    }
}
