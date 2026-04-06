package com.shinhan.esg_be.domain.social.service;

import com.shinhan.esg_be.domain.social.dto.response.EcoProductResponse;
import com.shinhan.esg_be.domain.social.repository.EcoProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EcoProductService {

    private final EcoProductRepository ecoProductRepository;

    public EcoProductResponse getProducts() {
        return new EcoProductResponse(ecoProductRepository.findActiveProducts());
    }
}
