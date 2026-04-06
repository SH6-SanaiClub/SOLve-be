package com.shinhan.esg_be.domain.point.repository;

import com.shinhan.esg_be.domain.point.entity.UserPoint;
import com.shinhan.esg_be.domain.point.entity.enums.PointCategory;
import com.shinhan.esg_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface UserPointRepository extends JpaRepository<UserPoint, Long> {

    long countByUserAndCategory(User user, PointCategory category);

    boolean existsByUserAndCategoryInAndCreatedAtBetween(
            User user,
            Collection<PointCategory> categories,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );

    List<UserPoint> findByUserAndCategoryAndCreatedAtBetweenOrderByCreatedAtAsc(
            User user,
            PointCategory category,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );

    List<UserPoint> findByUserAndCategoryInAndCreatedAtBetweenOrderByCreatedAtAsc(
            User user,
            Collection<PointCategory> categories,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );
}
