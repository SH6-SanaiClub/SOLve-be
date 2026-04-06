package com.shinhan.esg_be.domain.social.repository;

import com.shinhan.esg_be.domain.social.dto.response.EcoProductItemResponse;
import com.shinhan.esg_be.domain.social.entity.EcoProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EcoProductRepository extends JpaRepository<EcoProduct, Long> {

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
}
