package com.shinhan.esg_be.domain.admin.dto.response;

import com.shinhan.esg_be.domain.social.entity.UserDonation;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AdminDonationParticipantResponse {

    private Long userId;
    private String loginId;
    private String name;
    private Long amount;
    private LocalDateTime createdAt;

    public static AdminDonationParticipantResponse from(UserDonation userDonation) {
        return new AdminDonationParticipantResponse(
                userDonation.getUser().getUserId(),
                userDonation.getUser().getLoginId(),
                userDonation.getUser().getName(),
                userDonation.getPayment().getAmount(),
                userDonation.getCreatedAt()
        );
    }
}
