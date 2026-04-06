package com.shinhan.esg_be.domain.social.controller;

import com.shinhan.esg_be.domain.social.dto.response.EcoProductResponse;
import com.shinhan.esg_be.domain.social.service.EcoProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
}
