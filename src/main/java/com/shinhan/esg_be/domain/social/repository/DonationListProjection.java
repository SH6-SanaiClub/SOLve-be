package com.shinhan.esg_be.domain.social.repository;

import java.time.LocalDateTime;

public interface DonationListProjection {

    Long getDonationId();

    String getName();

    String getDescription();

    Long getTargetAmount();

    Long getCurrentAmount();

    String getImageUrl();

    LocalDateTime getStartDate();

    LocalDateTime getEndDate();

    Boolean getActive();

    Long getParticipantCount();
}
