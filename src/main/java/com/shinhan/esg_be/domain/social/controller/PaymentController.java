package com.shinhan.esg_be.domain.social.controller;

import com.shinhan.esg_be.domain.social.dto.request.PaymentPrepareRequest;
import com.shinhan.esg_be.domain.social.dto.request.PaymentVerifyRequest;
import com.shinhan.esg_be.domain.social.dto.request.ProductPaymentPrepareRequest;
import com.shinhan.esg_be.domain.social.dto.request.ProductPaymentVerifyRequest;
import com.shinhan.esg_be.domain.social.dto.response.PaymentPrepareResponse;
import com.shinhan.esg_be.domain.social.dto.response.PaymentVerifyResponse;
import com.shinhan.esg_be.domain.social.dto.response.ProductPaymentPrepareResponse;
import com.shinhan.esg_be.domain.social.dto.response.ProductPaymentVerifyResponse;
import com.shinhan.esg_be.domain.social.service.PaymentPrepareService;
import com.shinhan.esg_be.domain.social.service.PaymentVerifyService;
import com.shinhan.esg_be.domain.social.service.ProductPaymentPrepareService;
import com.shinhan.esg_be.domain.social.service.ProductPaymentVerifyService;
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
    private final PaymentVerifyService paymentVerifyService;
    private final ProductPaymentPrepareService productPaymentPrepareService;
    private final ProductPaymentVerifyService productPaymentVerifyService;

    @PostMapping("/prepare")
    public ResponseEntity<PaymentPrepareResponse> preparePayment(@RequestBody @Valid PaymentPrepareRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentPrepareService.preparePayment(request));
    }

    @PostMapping("/products/prepare")
    public ResponseEntity<ProductPaymentPrepareResponse> prepareProductPayment(
            @RequestBody @Valid ProductPaymentPrepareRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productPaymentPrepareService.preparePayment(request));
    }

    @PostMapping("/verify")
    public ResponseEntity<PaymentVerifyResponse> verifyPayment(@RequestBody @Valid PaymentVerifyRequest request) {
        return ResponseEntity.ok(paymentVerifyService.verifyDonationPayment(request));
    }

    @PostMapping("/products/verify")
    public ResponseEntity<ProductPaymentVerifyResponse> verifyProductPayment(
            @RequestBody @Valid ProductPaymentVerifyRequest request
    ) {
        return ResponseEntity.ok(productPaymentVerifyService.verifyProductPayment(request));
    }
}
