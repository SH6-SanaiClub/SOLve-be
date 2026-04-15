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

    @Override
    @Query("""
            select up
            from UserPoint up
            order by up.exchangeId asc
            """)
    List<UserPoint> findAll();

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

    @Query("""
            select up
            from UserPoint up
            left join fetch up.item i
            where up.user.userId = :userId
              and up.createdAt >= :startDateTime
            order by up.createdAt desc
            """)
    List<UserPoint> findPointHistoriesByUserIdAndStartDateTime(
            @Param("userId") Long userId,
            @Param("startDateTime") LocalDateTime startDateTime
    );

    long countByUserAndReason(User user, PointReason reason);

    boolean existsByUserAndReasonAndCreatedAtBetween(
            User user,
            PointReason reason,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );

    long countByItem_ItemId(Long itemId);

    @Query("""
            select up
            from UserPoint up
            join fetch up.user u
            where up.item.itemId = :itemId
              and up.reason = :reason
            order by up.createdAt desc
            """)
    List<UserPoint> findAllByItemIdAndReasonWithUser(
            @Param("itemId") Long itemId,
            @Param("reason") PointReason reason
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
