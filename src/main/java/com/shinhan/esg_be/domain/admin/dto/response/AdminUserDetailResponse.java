package com.shinhan.esg_be.domain.admin.dto.response;

import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.entity.enums.Grade;
import com.shinhan.esg_be.domain.user.entity.enums.UserType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AdminUserDetailResponse {

    private Long userId;
    private String loginId;
    private String name;
    private String email;
    private String phoneNumber;
    private LocalDate birthdate;
    private Integer eScore;
    private Integer sScore;
    private Integer gActivityScore;
    private Integer gRepaymentScore;
    private Integer totalScore;
    private Grade currentGrade;
    private UserType userType;
    private Integer totalPoints;
    private Integer abuseCount;
    private Boolean isActive;
    private Boolean isLinked;
    private LocalDateTime lastActivityDate;
    private LocalDateTime createdAt;

    public static AdminUserDetailResponse from(User user) {
        return new AdminUserDetailResponse(
                user.getUserId(),
                user.getLoginId(),
                user.getName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getBirthdate(),
                user.getEScore(),
                user.getSScore(),
                user.getGActivityScore(),
                user.getGRepaymentScore(),
                user.getTotalScore(),
                user.getCurrentGrade(),
                user.getUserType(),
                user.getTotalPoints(),
                user.getAbuseCount(),
                user.getIsActive(),
                user.getIsLinked(),
                user.getLastActivityDate(),
                user.getCreatedAt()
        );
    }
}
