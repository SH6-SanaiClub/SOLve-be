package com.shinhan.esg_be.domain.social.service;

import com.shinhan.esg_be.domain.social.dto.response.DonationDetailResponse;
import com.shinhan.esg_be.domain.social.dto.response.DonationItemResponse;
import com.shinhan.esg_be.domain.social.dto.response.DonationResponse;
import com.shinhan.esg_be.domain.social.dto.response.DonationSummaryResponse;
import com.shinhan.esg_be.domain.social.repository.DonationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DonationService {

    private final DonationRepository donationRepository;

    public DonationResponse getDonations() {
        List<DonationItemResponse> donations = donationRepository.findActiveDonationList(LocalDate.now().atStartOfDay());

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

    public DonationDetailResponse getDonation(Long donationId) {
        return donationRepository.findActiveDonationDetail(donationId, LocalDate.now().atStartOfDay())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "기부 캠페인을 찾을 수 없습니다."));
    }
}
