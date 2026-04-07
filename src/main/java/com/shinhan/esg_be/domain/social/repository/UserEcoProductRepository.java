package com.shinhan.esg_be.domain.social.repository;

import com.shinhan.esg_be.domain.social.entity.UserEcoProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface UserEcoProductRepository extends JpaRepository<UserEcoProduct, Long> {

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

    long countByUser_UserIdAndEcoProduct_ProductIdAndCreatedAtAfter(
            Long userId,
            Long productId,
            LocalDateTime since
    );
}
