package com.shinhan.esg_be.domain.social.service;

import com.shinhan.esg_be.domain.social.dto.response.DonationItemResponse;
import com.shinhan.esg_be.domain.social.dto.response.DonationResponse;
import com.shinhan.esg_be.domain.social.dto.response.DonationSummaryResponse;
import com.shinhan.esg_be.domain.social.repository.DonationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DonationService {

    private final DonationRepository donationRepository;

    public DonationResponse getDonations() {
        List<DonationItemResponse> donations = donationRepository.findActiveDonationList(LocalDate.now().atStartOfDay())
                .stream()
                .map(DonationItemResponse::from)
                .toList();

        long totalDonationAmount = donations.stream()
                .mapToLong(DonationItemResponse::getCurrentAmount)
                .sum();

        long totalParticipantCount = donations.stream()
                .mapToLong(DonationItemResponse::getParticipantCount)
                .sum();

        DonationSummaryResponse summary = new DonationSummaryResponse(
                totalDonationAmount,
                totalParticipantCount,
                donations.size()
        );

        return new DonationResponse(summary, donations);
    }
}
