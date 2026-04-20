package com.shinhan.esg_be.domain.volunteer.repository;

import com.shinhan.esg_be.domain.recommendation.dto.VolunteerCountProjection;
import com.shinhan.esg_be.domain.volunteer.dto.response.VolunteerHistoryItemResponse;
import com.shinhan.esg_be.domain.volunteer.entity.UserVolunteer;
import com.shinhan.esg_be.domain.volunteer.entity.enums.VolunteerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserVolunteerRepository extends JpaRepository<UserVolunteer, Long> {

    @Query("""
            SELECT COUNT(uv)
            FROM UserVolunteer uv
            WHERE uv.createdAt >= :startDateTime
              AND uv.createdAt < :endDateTime
            """)
    long countCreatedAtBetween(
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );

    long countByVolunteer_VolunteerId(Long volunteerId);

    @Query("""
            SELECT uv
            FROM UserVolunteer uv
            JOIN FETCH uv.user u
            WHERE uv.volunteer.volunteerId = :volunteerId
            ORDER BY uv.createdAt DESC
            """)
    List<UserVolunteer> findAllByVolunteerIdWithUser(@Param("volunteerId") Long volunteerId);

    boolean existsByUser_UserIdAndVolunteer_VolunteerIdAndStatus(
            Long userId, Long volunteerId, VolunteerStatus status
    );

    Optional<UserVolunteer> findByUser_UserIdAndVolunteer_VolunteerIdAndStatus(
            Long userId, Long volunteerId, VolunteerStatus status
    );

    Optional<UserVolunteer> findByUser_UserIdAndVolunteer_VolunteerId(
            Long userId, Long volunteerId
    );

    Optional<UserVolunteer> findByVolunteerApplicationsIdAndUser_UserIdAndStatus(
            Long volunteerApplicationId,
            Long userId,
            VolunteerStatus status
    );

    @Query("""
            select new com.shinhan.esg_be.domain.volunteer.dto.response.VolunteerApplicationItemResponse(
                uv.volunteerApplicationsId,
                v.volunteerId,
                v.name,
                v.description,
                v.activityDate,
                v.location,
                v.capacity,
                v.currentEnrolled,
                v.volunteerHour,
                v.organization
            )
            from UserVolunteer uv
            join uv.volunteer v
            where uv.user.userId = :userId
              and uv.status = com.shinhan.esg_be.domain.volunteer.entity.enums.VolunteerStatus.APPLIED
              and v.isActive = true
              and v.activityDate > :now
            order by v.activityDate asc
            """)
    List<com.shinhan.esg_be.domain.volunteer.dto.response.VolunteerApplicationItemResponse> findApplicationVolunteersByUserId(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now
    );

    @Query("""
            select new com.shinhan.esg_be.domain.volunteer.dto.response.VolunteerHistoryItemResponse(
                uv.volunteerApplicationsId,
                v.volunteerId,
                v.organization,
                v.name,
                v.activityDate,
                uv.status,
                uv.checkInAt,
                uv.checkOutAt,
                uv.volunteerHour,
                v.volunteerHour
            )
            from UserVolunteer uv
            join uv.volunteer v
            where uv.user.userId = :userId
              and v.activityDate < :now
            order by v.activityDate desc
            """)
    List<VolunteerHistoryItemResponse> findVolunteerHistoryByUserId(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now
    );

    List<UserVolunteer> findAllByVolunteerApplicationsIdIn(List<Long> volunteerApplicationIds);

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
              AND uv.status = com.shinhan.esg_be.domain.volunteer.entity.enums.VolunteerStatus.COMPLETED
            GROUP BY uv.volunteer.volunteerId
            """)
    List<VolunteerCountProjection> countGroupByVolunteerSince(
            @Param("since") LocalDateTime since
    );
}
