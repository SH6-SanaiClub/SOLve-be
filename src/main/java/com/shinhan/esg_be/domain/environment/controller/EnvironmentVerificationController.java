package com.shinhan.esg_be.domain.environment.controller;

import com.shinhan.esg_be.domain.environment.dto.request.EnvironmentVerificationCreateRequest;
import com.shinhan.esg_be.domain.environment.dto.response.EnvironmentVerificationAvailabilityResponse;
import com.shinhan.esg_be.domain.environment.dto.response.EnvironmentVerificationResponse;
import com.shinhan.esg_be.domain.environment.service.EnvironmentVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/esg/e")
@RequiredArgsConstructor
public class EnvironmentVerificationController {

    private final EnvironmentVerificationService environmentVerificationService;

    @GetMapping("/verifications/availability")
    public ResponseEntity<List<EnvironmentVerificationAvailabilityResponse>> getVerificationAvailability() {
        return ResponseEntity.ok(environmentVerificationService.getVerificationAvailability());
    }

    @PostMapping(value = "/verifications", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EnvironmentVerificationResponse> createVerification(
            @ModelAttribute @Valid EnvironmentVerificationCreateRequest request
    ) {
        return ResponseEntity.ok(environmentVerificationService.createVerification(request));
    }
}
