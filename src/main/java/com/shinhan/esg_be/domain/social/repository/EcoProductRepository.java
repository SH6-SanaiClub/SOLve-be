package com.shinhan.esg_be.domain.social.repository;

import com.shinhan.esg_be.domain.social.dto.response.EcoProductDetailResponse;
import com.shinhan.esg_be.domain.social.dto.response.EcoProductItemResponse;
import com.shinhan.esg_be.domain.social.entity.EcoProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EcoProductRepository extends JpaRepository<EcoProduct, Long> {

    List<EcoProduct> findByIsActiveTrue();
    List<EcoProduct> findByIsActiveTrueAndStockGreaterThan(Integer stock);
    Optional<EcoProduct> findByProductIdAndIsActiveTrue(Long productId);

    @Query("""
            select new com.shinhan.esg_be.domain.social.dto.response.EcoProductItemResponse(
                e.productId,
                e.name,
                e.category,
                e.price,
                e.imageUrl,
                e.description,
                e.stock
            )
            from EcoProduct e
            where e.isActive = true
            order by e.productId desc
            """)
    List<EcoProductItemResponse> findActiveProducts();

    @Query("""
            select new com.shinhan.esg_be.domain.social.dto.response.EcoProductDetailResponse(
                e.productId,
                e.name,
                e.category,
                e.price,
                e.imageUrl,
                e.description,
                e.stock
            )
            from EcoProduct e
            where e.productId = :productId
              and e.isActive = true
            """)
    Optional<EcoProductDetailResponse> findActiveProductDetail(@Param("productId") Long productId);
}
