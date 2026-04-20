package com.shinhan.esg_be.domain.social.repository;

import com.shinhan.esg_be.domain.recommendation.dto.ProductCountProjection;
import com.shinhan.esg_be.domain.social.entity.UserEcoProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface UserEcoProductRepository extends JpaRepository<UserEcoProduct, Long> {

    @Query("""
            SELECT COUNT(uep)
            FROM UserEcoProduct uep
            WHERE uep.createdAt >= :startDateTime
              AND uep.createdAt < :endDateTime
            """)
    long countCreatedAtBetween(
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );

    long countByEcoProduct_ProductId(Long productId);

    // 최근 N일 상품구매 횟수 (미활동 위험, 90일 횟수)
    @Query("""
            SELECT COUNT(uep) FROM UserEcoProduct uep
            WHERE uep.user.userId = :userId
              AND uep.createdAt >= :since
            """)
    long countSince(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since
    );

    @Query("""
            SELECT COUNT(uep) FROM UserEcoProduct uep
            WHERE uep.user.userId = :userId
              AND uep.createdAt >= :startOfDay
            """)
    long countTodayByUser(
            @Param("userId") Long userId,
            @Param("startOfDay") LocalDateTime startOfDay
    );

    long countByUser_UserIdAndEcoProduct_ProductIdAndCreatedAtAfter(
            Long userId,
            Long productId,
            LocalDateTime since
    );

    @Query("""
            SELECT uep.ecoProduct.productId AS productId, COUNT(uep) AS count
            FROM UserEcoProduct uep
            WHERE uep.createdAt >= :since
            GROUP BY uep.ecoProduct.productId
            """)
    List<ProductCountProjection> countGroupByProductSince(
            @Param("since") LocalDateTime since
    );
}
