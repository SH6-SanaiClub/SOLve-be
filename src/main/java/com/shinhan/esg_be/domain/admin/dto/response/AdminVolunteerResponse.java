package com.shinhan.esg_be.domain.admin.dto.response;

import com.shinhan.esg_be.domain.volunteer.entity.Volunteer;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class AdminVolunteerResponse {

    private Long volunteerId;
    private String name;
    private String description;
    private LocalDateTime activityDate;
    private String location;
    private Integer capacity;
    private Integer currentEnrolled;
    private Integer volunteerHour;
    private String organization;
    private String qrToken;
    private Double latitude;
    private Double longitude;
    private Boolean isActive;
    private Long participantCount;
    private List<AdminVolunteerParticipantResponse> participants;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AdminVolunteerResponse from(
            Volunteer volunteer,
            long participantCount,
            List<AdminVolunteerParticipantResponse> participants
    ) {
        return new AdminVolunteerResponse(
                volunteer.getVolunteerId(),
                volunteer.getName(),
                volunteer.getDescription(),
                volunteer.getActivityDate(),
                volunteer.getLocation(),
                volunteer.getCapacity(),
                volunteer.getCurrentEnrolled(),
                volunteer.getVolunteerHour(),
                volunteer.getOrganization(),
                volunteer.getQrToken(),
                volunteer.getLatitude(),
                volunteer.getLongitude(),
                volunteer.getIsActive(),
                participantCount,
                participants,
                volunteer.getCreatedAt(),
                volunteer.getUpdatedAt()
        );
    }
}
