package com.shinhan.esg_be.domain.social.controller;

import com.shinhan.esg_be.domain.social.dto.response.EcoProductDetailResponse;
import com.shinhan.esg_be.domain.social.dto.response.EcoProductPurchaseResponse;
import com.shinhan.esg_be.domain.social.dto.response.EcoProductResponse;
import com.shinhan.esg_be.domain.social.service.EcoProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/esg/s")
@RequiredArgsConstructor
public class EcoProductController {

    private final EcoProductService ecoProductService;

    @GetMapping("/products")
    public ResponseEntity<EcoProductResponse> getProducts() {
        return ResponseEntity.ok(ecoProductService.getProducts());
    }

    @GetMapping("/products/{productId}")
    public ResponseEntity<EcoProductDetailResponse> getProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(ecoProductService.getProduct(productId));
    }

    @GetMapping("/products/purchases")
    public ResponseEntity<EcoProductPurchaseResponse> getPurchases() {
        return ResponseEntity.ok(ecoProductService.getPurchases());
    }
}
