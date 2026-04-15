package com.shinhan.esg_be.domain.volunteer.entity;

import com.shinhan.esg_be.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "volunteer")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Volunteer extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "volunteer_id")
    private Long volunteerId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "activity_date", nullable = false)
    private LocalDateTime activityDate;

    @Column(nullable = false)
    private String location;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(nullable = false)
    private Integer capacity;

    @Column(name = "current_enrolled", nullable = false)
    private Integer currentEnrolled;

    @Column(name = "volunteer_hour", nullable = false)
    private Integer volunteerHour;

    @Column(nullable = false)
    private String organization;

    @Column(name = "qr_token", unique = true)
    private String qrToken;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    public static Volunteer create(
            String name,
            String description,
            LocalDateTime activityDate,
            String location,
            Boolean isActive,
            Integer capacity,
            Integer volunteerHour,
            String organization,
            String qrToken,
            Double latitude,
            Double longitude
    ) {
        Volunteer volunteer = new Volunteer();
        volunteer.name = name;
        volunteer.description = description;
        volunteer.activityDate = activityDate;
        volunteer.location = location;
        volunteer.isActive = isActive;
        volunteer.capacity = capacity;
        volunteer.currentEnrolled = 0;
        volunteer.volunteerHour = volunteerHour;
        volunteer.organization = organization;
        volunteer.qrToken = qrToken;
        volunteer.latitude = latitude;
        volunteer.longitude = longitude;
        return volunteer;
    }

    public void update(
            String name,
            String description,
            LocalDateTime activityDate,
            String location,
            Integer capacity,
            Integer volunteerHour,
            String organization,
            String qrToken,
            Double latitude,
            Double longitude
    ) {
        this.name = name;
        this.description = description;
        this.activityDate = activityDate;
        this.location = location;
        this.capacity = capacity;
        this.volunteerHour = volunteerHour;
        this.organization = organization;
        this.qrToken = qrToken;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void updateStatus(boolean isActive) {
        this.isActive = isActive;
    }
}
