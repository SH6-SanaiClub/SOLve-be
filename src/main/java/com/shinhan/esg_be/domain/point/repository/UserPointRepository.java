package com.shinhan.esg_be.domain.point.repository;

import com.shinhan.esg_be.domain.point.entity.UserPoint;
import com.shinhan.esg_be.domain.point.entity.enums.PointReason;
import com.shinhan.esg_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface UserPointRepository extends JpaRepository<UserPoint, Long> {

    boolean existsByExchangeCode(String exchangeCode);

    @Query("""
            select up
            from UserPoint up
            join fetch up.item i
            where up.user.userId = :userId
              and up.reason = :reason
            order by up.createdAt desc
            """)
    List<UserPoint> findPointShopPurchasesByUserIdAndReason(
            @Param("userId") Long userId,
            @Param("reason") PointReason reason
    );

    long countByUserAndReason(User user, PointReason reason);

    boolean existsByUserAndReasonAndCreatedAtBetween(
            User user,
            PointReason reason,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );

    boolean existsByUserAndReasonInAndCreatedAtBetween(
            User user,
            Collection<PointReason> reasons,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );

    List<UserPoint> findByUserAndReasonAndCreatedAtBetweenOrderByCreatedAtAsc(
            User user,
            PointReason reason,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );

    List<UserPoint> findByUserAndReasonInAndCreatedAtBetweenOrderByCreatedAtAsc(
            User user,
            Collection<PointReason> reasons,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );
}
