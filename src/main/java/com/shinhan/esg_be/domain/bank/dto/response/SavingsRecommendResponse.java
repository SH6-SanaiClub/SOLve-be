package com.shinhan.esg_be.domain.bank.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SavingsRecommendResponse {

    @JsonProperty("isNewUser")
    private boolean isNewUser;
    private RecommendItem recommendation;

    @Getter
    @Builder
    public static class RecommendItem {
        private Long productId;
        private String productName;
        private int matchScore;
        private String expectedMaxRate;
        private String reason;
        private String actionable;
        @JsonProperty("isNewUserRecommend")
        private boolean isNewUserRecommend;
        @JsonProperty("isAlreadyJoined")
        private boolean isAlreadyJoined;
        @JsonProperty("isIneligible")
        private boolean isIneligible;
    }
}
