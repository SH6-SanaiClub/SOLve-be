package com.shinhan.esg_be.domain.volunteer.repository;

import com.shinhan.esg_be.domain.volunteer.dto.response.VolunteerDetailResponse;
import com.shinhan.esg_be.domain.volunteer.dto.response.VolunteerItemResponse;
import com.shinhan.esg_be.domain.volunteer.entity.Volunteer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VolunteerRepository extends JpaRepository<Volunteer, Long> {

    @Query("""
            select new com.shinhan.esg_be.domain.volunteer.dto.response.VolunteerItemResponse(
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
            from Volunteer v
            where v.isActive = true
              and v.activityDate > :now
              and v.currentEnrolled < v.capacity
            order by v.activityDate asc
            """)
    List<VolunteerItemResponse> findActiveVolunteerList(@Param("now") LocalDateTime now);

    @Query("""
            select v
            from Volunteer v
            where v.volunteerId = :volunteerId
              and v.isActive = true
              and v.activityDate > :now
            """)
    Optional<Volunteer> findActiveVolunteerDetail(
            @Param("volunteerId") Long volunteerId,
            @Param("now") LocalDateTime now
    );

    @Query("""
            select v
            from Volunteer v
            where v.volunteerId = :volunteerId
              and v.isActive = true
              and v.activityDate > :now
            """)
    Optional<Volunteer> findApplicableVolunteer(
            @Param("volunteerId") Long volunteerId,
            @Param("now") LocalDateTime now
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Volunteer v
            set v.currentEnrolled = v.currentEnrolled + 1
            where v.volunteerId = :volunteerId
              and v.currentEnrolled < v.capacity
            """)
    int increaseCurrentEnrolled(@Param("volunteerId") Long volunteerId);

    @Query("""
            SELECT v
            FROM Volunteer v
            WHERE v.isActive = true
              AND v.activityDate > :now
              AND v.currentEnrolled < v.capacity
            ORDER BY v.activityDate ASC
            """)
    List<Volunteer> findActiveVolunteers(@Param("now") LocalDateTime now);
}
