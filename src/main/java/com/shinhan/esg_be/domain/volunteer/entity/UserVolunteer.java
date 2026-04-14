package com.shinhan.esg_be.domain.volunteer.entity;

import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.volunteer.entity.enums.VolunteerStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.EntityListeners;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_volunteer")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserVolunteer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "volunteer_applications_id")
    private Long volunteerApplicationsId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "volunteer_id", nullable = false)
    private Volunteer volunteer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VolunteerStatus status = VolunteerStatus.APPLIED;

    @Column(name = "volunteer_hour", nullable = false)
    private Integer volunteerHour;

    @Column(name = "check_in_at")
    private LocalDateTime checkInAt;

    @Column(name = "check_out_at")
    private LocalDateTime checkOutAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static UserVolunteer create(User user, Volunteer volunteer) {
        UserVolunteer userVolunteer = new UserVolunteer();
        userVolunteer.user = user;
        userVolunteer.volunteer = volunteer;
        userVolunteer.status = VolunteerStatus.APPLIED;
        userVolunteer.volunteerHour = volunteer.getVolunteerHour();
        return userVolunteer;
    }

    public void markCheckIn(LocalDateTime checkedInAt, boolean late) {
        this.checkInAt = checkedInAt;
        this.status = late ? VolunteerStatus.INCOMPLETE : VolunteerStatus.ATTENDED;
    }

    public void markCheckOut(LocalDateTime checkedOutAt, boolean completed) {
        this.checkOutAt = checkedOutAt;
        this.status = completed ? VolunteerStatus.COMPLETED : VolunteerStatus.INCOMPLETE;
    }

    public void markNoShow() {
        this.status = VolunteerStatus.NOSHOW;
    }

    public void markIncomplete() {
        this.status = VolunteerStatus.INCOMPLETE;
    }
}
