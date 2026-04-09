package com.shinhan.esg_be.domain.social.service;

import com.shinhan.esg_be.domain.point.service.result.ApplyActivityPointResult;
import com.shinhan.esg_be.domain.reward.service.RewardService;
import com.shinhan.esg_be.domain.reward.service.command.ApplyActivityRewardCommand;
import com.shinhan.esg_be.domain.reward.service.result.ApplyActivityRewardResult;
import com.shinhan.esg_be.domain.social.dto.request.PaymentVerifyRequest;
import com.shinhan.esg_be.domain.social.dto.response.PaymentVerifyResponse;
import com.shinhan.esg_be.domain.social.entity.Donation;
import com.shinhan.esg_be.domain.social.entity.Payment;
import com.shinhan.esg_be.domain.social.entity.UserDonation;
import com.shinhan.esg_be.domain.social.repository.DonationRepository;
import com.shinhan.esg_be.domain.social.repository.PaymentRepository;
import com.shinhan.esg_be.domain.social.repository.UserDonationRepository;
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

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentVerifyService {

    private static final long MIN_DONATION_AMOUNT = 30_000L;

    private final AuthContext authContext;
    private final PortOnePaymentService portOnePaymentService;
    private final DonationRepository donationRepository;
    private final PaymentRepository paymentRepository;
    private final UserDonationRepository userDonationRepository;
    private final UserRepository userRepository;
    private final RewardService rewardService;

    public PaymentVerifyResponse verifyDonationPayment(PaymentVerifyRequest request) {
        Long userId = authContext.currentUserId();
        LocalDateTime activityDateTime = LocalDateTime.now();
        LocalDateTime baseDateTime = activityDateTime.toLocalDate().atStartOfDay();

        Donation donation = donationRepository
                .findByDonationIdAndIsActiveTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        request.getDonationId(),
                        baseDateTime,
                        baseDateTime
                )
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "기부 캠페인을 찾을 수 없습니다."));

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
        if (verifiedPayment.amount() < MIN_DONATION_AMOUNT) {
            throw new BadRequestException("기부 금액은 30000원 이상이어야 합니다.");
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

        userDonationRepository.save(UserDonation.create(payment, donation, user));

        ApplyActivityRewardResult rewardResult = rewardService.applyActivityReward(
                new ApplyActivityRewardCommand(
                        userId,
                        ActivityType.DONATION,
                        BigDecimal.valueOf(payment.getAmount()),
                        activityDateTime
                )
        );

        ApplyActivityPointResult pointResult = rewardResult.pointResult();

        return new PaymentVerifyResponse(
                payment.getPaymentId(),
                donation.getDonationId(),
                donation.getName(),
                payment.getAmount(),
                payment.getPaymentStatus(),
                pointResult.activityPoint() + pointResult.bonusPoint(),
                pointResult.pointAfter()
        );
    }
}
