package com.shinhan.esg_be.domain.social.service;

import com.shinhan.esg_be.domain.social.dto.request.ProductPaymentPrepareRequest;
import com.shinhan.esg_be.domain.social.dto.response.ProductPaymentPrepareResponse;
import com.shinhan.esg_be.domain.social.entity.EcoProduct;
import com.shinhan.esg_be.domain.social.repository.EcoProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductPaymentPrepareService {

    private final EcoProductRepository ecoProductRepository;

    public ProductPaymentPrepareResponse preparePayment(ProductPaymentPrepareRequest request) {
        EcoProduct product = ecoProductRepository.findByProductIdAndIsActiveTrue(request.getProductId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "상품을 찾을 수 없습니다."));

        if (product.getStock() == null || product.getStock() <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "품절된 상품은 결제할 수 없습니다.");
        }

        return new ProductPaymentPrepareResponse(
                createMerchantUid(),
                product.getProductId(),
                product.getName(),
                product.getStoreName(),
                product.getCategory(),
                product.getPrice()
        );
    }

    private String createMerchantUid() {
        return "PRODUCT-" + UUID.randomUUID();
    }
}
