package com.shinhan.esg_be.domain.social.controller;

import com.shinhan.esg_be.domain.social.dto.response.DonationResponse;
import com.shinhan.esg_be.domain.social.service.DonationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/esg/s")
@RequiredArgsConstructor
public class DonationController {

    private final DonationService donationService;

    @GetMapping("/donations")
    public ResponseEntity<DonationResponse> getDonations() {
        return ResponseEntity.ok(donationService.getDonations());
    }
}
