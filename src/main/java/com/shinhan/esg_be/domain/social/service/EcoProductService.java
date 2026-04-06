package com.shinhan.esg_be.domain.social.service;

import com.shinhan.esg_be.domain.social.dto.response.EcoProductDetailResponse;
import com.shinhan.esg_be.domain.social.dto.response.EcoProductResponse;
import com.shinhan.esg_be.domain.social.repository.EcoProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EcoProductService {

    private final EcoProductRepository ecoProductRepository;

    public EcoProductResponse getProducts() {
        return new EcoProductResponse(ecoProductRepository.findActiveProducts());
    }

    public EcoProductDetailResponse getProduct(Long productId) {
        return ecoProductRepository.findActiveProductDetail(productId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "상품을 찾을 수 없습니다."));
    }
}
