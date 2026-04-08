package com.shinhan.esg_be.domain.social.controller;

import com.shinhan.esg_be.domain.social.dto.request.PaymentPrepareRequest;
import com.shinhan.esg_be.domain.social.dto.response.PaymentPrepareResponse;
import com.shinhan.esg_be.domain.social.service.PaymentPrepareService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentPrepareService paymentPrepareService;

    @PostMapping("/prepare")
    public ResponseEntity<PaymentPrepareResponse> preparePayment(@RequestBody @Valid PaymentPrepareRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentPrepareService.preparePayment(request));
    }
}
