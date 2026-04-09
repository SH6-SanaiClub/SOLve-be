package com.shinhan.esg_be.domain.social.service;

import com.shinhan.esg_be.domain.social.dto.request.PaymentPrepareRequest;
import com.shinhan.esg_be.domain.social.dto.response.PaymentPrepareResponse;
import com.shinhan.esg_be.domain.social.entity.Donation;
import com.shinhan.esg_be.domain.social.repository.DonationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentPrepareService {

    private static final long MIN_DONATION_AMOUNT = 30_000L;

    private final DonationRepository donationRepository;

    public PaymentPrepareResponse preparePayment(PaymentPrepareRequest request) {
        validateAmount(request.getAmount());
        LocalDateTime baseDateTime = LocalDate.now().atStartOfDay();

        Donation donation = donationRepository
                .findByDonationIdAndIsActiveTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        request.getDonationId(),
                        baseDateTime,
                        baseDateTime
                )
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "기부 캠페인을 찾을 수 없습니다."));

        return new PaymentPrepareResponse(
                createMerchantUid(),
                donation.getDonationId(),
                donation.getName(),
                request.getAmount()
        );
    }

    private void validateAmount(Long amount) {
        if (amount == null || amount < MIN_DONATION_AMOUNT) {
            throw new ResponseStatusException(BAD_REQUEST, "기부 금액은 30000원 이상이어야 합니다.");
        }
    }

    private String createMerchantUid() {
        return "DONATION-" + UUID.randomUUID();
    }
}
