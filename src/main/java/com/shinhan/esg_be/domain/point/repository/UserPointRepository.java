package com.shinhan.esg_be.domain.point.repository;

import com.shinhan.esg_be.domain.point.entity.UserPoint;
import com.shinhan.esg_be.domain.point.entity.enums.PointReason;
import com.shinhan.esg_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface UserPointRepository extends JpaRepository<UserPoint, Long> {

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
