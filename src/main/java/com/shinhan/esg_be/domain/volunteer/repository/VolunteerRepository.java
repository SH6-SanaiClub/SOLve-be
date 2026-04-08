package com.shinhan.esg_be.domain.volunteer.repository;

import com.shinhan.esg_be.domain.volunteer.entity.Volunteer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface VolunteerRepository extends JpaRepository<Volunteer, Long> {

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
