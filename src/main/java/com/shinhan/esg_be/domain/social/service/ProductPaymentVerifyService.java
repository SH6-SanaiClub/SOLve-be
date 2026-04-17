package com.shinhan.esg_be.domain.social.service;

import com.shinhan.esg_be.domain.point.service.result.ApplyActivityPointResult;
import com.shinhan.esg_be.domain.reward.service.RewardService;
import com.shinhan.esg_be.domain.reward.service.command.ApplyActivityRewardCommand;
import com.shinhan.esg_be.domain.reward.service.result.ApplyActivityRewardResult;
import com.shinhan.esg_be.domain.social.dto.request.ProductPaymentVerifyRequest;
import com.shinhan.esg_be.domain.social.dto.response.ProductPaymentVerifyResponse;
import com.shinhan.esg_be.domain.social.entity.EcoProduct;
import com.shinhan.esg_be.domain.social.entity.Payment;
import com.shinhan.esg_be.domain.social.entity.UserEcoProduct;
import com.shinhan.esg_be.domain.social.repository.EcoProductRepository;
import com.shinhan.esg_be.domain.social.repository.PaymentRepository;
import com.shinhan.esg_be.domain.social.repository.UserEcoProductRepository;
import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.repository.UserRepository;
import com.shinhan.esg_be.global.common.enums.ActivityType;
import com.shinhan.esg_be.global.exception.BadRequestException;
import com.shinhan.esg_be.global.security.AuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductPaymentVerifyService {

    private final AuthContext authContext;
    private final PortOnePaymentService portOnePaymentService;
    private final EcoProductRepository ecoProductRepository;
    private final PaymentRepository paymentRepository;
    private final UserEcoProductRepository userEcoProductRepository;
    private final UserRepository userRepository;
    private final RewardService rewardService;

    public ProductPaymentVerifyResponse verifyProductPayment(ProductPaymentVerifyRequest request) {
        Long userId = authContext.currentUserId();
        LocalDateTime activityDateTime = LocalDateTime.now();

        EcoProduct product = ecoProductRepository.findByProductIdAndIsActiveTrue(request.getProductId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "상품을 찾을 수 없습니다."));

        if (product.getStock() == null || product.getStock() <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "품절된 상품은 결제할 수 없습니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "사용자를 찾을 수 없습니다."));

        if (paymentRepository.existsByImpUid(request.getImpUid())) {
            throw new BadRequestException("이미 처리된 결제입니다.");
        }
        if (paymentRepository.existsByMerchantId(request.getMerchantUid())) {
            throw new BadRequestException("이미 사용된 merchantUid입니다.");
        }

        PortOnePaymentService.VerifiedPayment verifiedPayment =
                portOnePaymentService.verify(request.getImpUid());

        if (!request.getMerchantUid().equals(verifiedPayment.merchantUid())) {
            throw new BadRequestException("merchantUid가 일치하지 않습니다.");
        }
        if (!product.getPrice().equals(verifiedPayment.amount())) {
            throw new BadRequestException("상품 결제 금액이 일치하지 않습니다.");
        }

        Payment payment = paymentRepository.save(Payment.create(
                verifiedPayment.impUid(),
                verifiedPayment.merchantUid(),
                verifiedPayment.amount(),
                verifiedPayment.paymentMethod(),
                verifiedPayment.paymentStatus(),
                verifiedPayment.paidAt(),
                verifiedPayment.failedReason(),
                verifiedPayment.buyerName(),
                verifiedPayment.buyerEmail(),
                verifiedPayment.buyerTel(),
                verifiedPayment.pgProvider(),
                verifiedPayment.pgTid(),
                verifiedPayment.cardName(),
                verifiedPayment.cardNumber(),
                verifiedPayment.receiptUrl()
        ));

        int updatedRows = ecoProductRepository.decreaseStock(product.getProductId());
        if (updatedRows == 0) {
            throw new ResponseStatusException(BAD_REQUEST, "재고가 부족하여 결제를 완료할 수 없습니다.");
        }

        userEcoProductRepository.save(UserEcoProduct.create(
                payment,
                product,
                user,
                request.getDeliveryAddress()
        ));

        ApplyActivityRewardResult rewardResult = rewardService.applyActivityReward(
                new ApplyActivityRewardCommand(
                        userId,
                        ActivityType.PURCHASE,
                        BigDecimal.valueOf(payment.getAmount()),
                        activityDateTime
                )
        );

        ApplyActivityPointResult pointResult = rewardResult.pointResult();

        return new ProductPaymentVerifyResponse(
                payment.getPaymentId(),
                product.getProductId(),
                product.getName(),
                product.getStoreName(),
                payment.getAmount(),
                payment.getPaymentStatus(),
                pointResult.activityPoint() + pointResult.bonusPoint(),
                pointResult.pointAfter()
        );
    }
}
