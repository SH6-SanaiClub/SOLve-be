package com.shinhan.esg_be.domain.admin.dto.response;

import com.shinhan.esg_be.domain.social.entity.Donation;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class AdminDonationResponse {

    private Long donationId;
    private String name;
    private String summary;
    private String organization;
    private String description;
    private Long targetAmount;
    private Long currentAmount;
    private String imageUrl;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean isActive;
    private Long participantCount;
    private List<AdminDonationParticipantResponse> participants;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AdminDonationResponse from(
            Donation donation,
            long participantCount,
            List<AdminDonationParticipantResponse> participants
    ) {
        return new AdminDonationResponse(
                donation.getDonationId(),
                donation.getName(),
                donation.getSummary(),
                donation.getOrganization(),
                donation.getDescription(),
                donation.getTargetAmount(),
                donation.getCurrentAmount(),
                donation.getImageUrl(),
                donation.getStartDate(),
                donation.getEndDate(),
                donation.getIsActive(),
                participantCount,
                participants,
                donation.getCreatedAt(),
                donation.getUpdatedAt()
        );
    }
}
