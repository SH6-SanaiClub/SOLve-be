package com.shinhan.esg_be.domain.volunteer.repository;

import com.shinhan.esg_be.domain.recommendation.dto.VolunteerCountProjection;
import com.shinhan.esg_be.domain.volunteer.entity.UserVolunteer;
import com.shinhan.esg_be.domain.volunteer.entity.enums.VolunteerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface UserVolunteerRepository extends JpaRepository<UserVolunteer, Long> {

    // 특정 봉사 신청 여부 (하드 필터 - 중복 신청 방지)
    boolean existsByUser_UserIdAndVolunteer_VolunteerIdAndStatusNot(
            Long userId, Long volunteerId, VolunteerStatus status
    );

    @Query("""
            SELECT COUNT(uv)
            FROM UserVolunteer uv
            WHERE uv.user.userId = :userId
              AND uv.status = com.shinhan.esg_be.domain.volunteer.entity.enums.VolunteerStatus.COMPLETED
              AND uv.createdAt >= :since
            """)
    long countCompletedSince(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since
    );

    long countByUser_UserIdAndVolunteer_VolunteerIdAndCreatedAtAfter(
            Long userId,
            Long volunteerId,
            LocalDateTime since
    );

    @Query("""
            SELECT uv.volunteer.volunteerId AS volunteerId, COUNT(uv) AS count
            FROM UserVolunteer uv
            WHERE uv.createdAt >= :since
            GROUP BY uv.volunteer.volunteerId
            """)
    List<VolunteerCountProjection> countGroupByVolunteerSince(
            @Param("since") LocalDateTime since
    );
}
