package com.shinhan.esg_be.domain.user.entity;

import com.shinhan.esg_be.domain.user.entity.enums.Grade;
import com.shinhan.esg_be.domain.user.entity.enums.UserType;
import com.shinhan.esg_be.global.common.BaseTimeEntity;
import com.shinhan.esg_be.global.common.enums.ScoreCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "\"user\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "login_id", nullable = false, unique = true)
    private String loginId;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(name = "phone_number", unique = true, nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private LocalDate birthdate;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "ci_di", unique = true, nullable = false)
    private String ciDi;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false, length = 20)
    private UserType userType = UserType.ALL_ROUNDER;

    @Column(name = "abuse_count", nullable = false)
    private Integer abuseCount = 0;

    @Column(name = "e_score", nullable = false)
    private Integer eScore = 50;

    @Column(name = "s_score", nullable = false)
    private Integer sScore = 250;

    @Column(name = "g_activity_score", nullable = false)
    private Integer gActivityScore = 100;

    @Column(name = "g_repayment_score", nullable = false)
    private Integer gRepaymentScore = 100;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_grade", nullable = false, length = 20)
    private Grade currentGrade = Grade.SEED;

    @Column(name = "total_points", nullable = false)
    private Integer totalPoints = 0;

    @Column(name = "last_activity_date")
    private LocalDateTime lastActivityDate;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "is_linked", nullable = false)
    private Boolean isLinked = false;

    @Column(name = "is_survey_completed", nullable = false)
    private Boolean isSurveyCompleted = false;

    public static User create(String loginId, String password, String name, String email,
                              String phoneNumber, LocalDate birthdate, String ciDi) {
        User user = new User();
        user.loginId = loginId;
        user.password = password;
        user.name = name;
        user.email = email;
        user.phoneNumber = phoneNumber;
        user.birthdate = birthdate;
        user.ciDi = ciDi;
        user.eScore = 50;
        user.sScore = 250;
        user.gActivityScore = 100;
        user.gRepaymentScore = 100;
        user.userType = UserType.ALL_ROUNDER;
        user.currentGrade = Grade.SEED;
        user.abuseCount = 0;
        user.totalPoints = 0;
        user.isActive = true;
        user.isLinked = false;
        user.isSurveyCompleted = false;

        return user;
    }

    // 본인인증 후 새 휴대폰 번호와 ciDi를 함께 변경
    public void updatePhoneNumberAndCiDi(String phoneNumber, String ciDi) {
        this.phoneNumber = phoneNumber;
        this.ciDi = ciDi;
    }

    // 이메일 주소 변경
    public void updateEmail(String email) {
        this.email = email;
    }

    // 암호화된 비밀번호 값으로 변경
    public void updatePassword(String password) {
        this.password = password;
    }

    // 회원탈퇴 시 계정을 비활성 상태로 변경
    public void withdraw() {
        this.isActive = false;
    }

    public void applyScore(ScoreCategory scoreCategory, int scoreDelta) {
        if (scoreDelta == 0) {
            return;
        }

        switch (scoreCategory) {
            case E -> eScore += scoreDelta;
            case S -> sScore += scoreDelta;
            case G_ACTIVITY -> gActivityScore += scoreDelta;
            case G_REPAYMENT -> gRepaymentScore += scoreDelta;
        }

        recalculateGrade();
    }

    public void initializeScore(ScoreCategory scoreCategory, int score) {
        switch (scoreCategory) {
            case E -> eScore = score;
            case S -> sScore = score;
            case G_ACTIVITY -> gActivityScore = score;
            case G_REPAYMENT -> gRepaymentScore = score;
        }

        recalculateGrade();
    }

    public void applyPoint(int pointDelta) {
        if (pointDelta == 0) {
            return;
        }
        totalPoints += pointDelta;
    }

    public void increaseAbuseCount() {
        abuseCount += 1;
    }

    public void blockLoan() {
        // Loan blocking is currently derived from abuseCount.
    }

    public boolean getIsLoanBlocked() {
        return abuseCount > 0;
    }

    public void updateLastActivityDate(LocalDateTime activityDateTime) {
        this.lastActivityDate = activityDateTime;
    }

    public void completeSurvey(UserType userType) {
        this.userType = userType;
        this.isSurveyCompleted = true;
    }

    public int getScore(ScoreCategory scoreCategory) {
        return switch (scoreCategory) {
            case E -> eScore;
            case S -> sScore;
            case G_ACTIVITY -> gActivityScore;
            case G_REPAYMENT -> gRepaymentScore;
        };
    }

    public int getTotalScore() {
        return eScore + sScore + gActivityScore + gRepaymentScore;
    }

    public void recalculateGrade() {
        int totalScore = getTotalScore();
        if (totalScore >= 900) {
            currentGrade = Grade.EARTH;
            return;
        }
        if (totalScore >= 800) {
            currentGrade = Grade.FOREST;
            return;
        }
        if (totalScore >= 700) {
            currentGrade = Grade.TREE;
            return;
        }
        if (totalScore >= 600) {
            currentGrade = Grade.SPROUT;
            return;
        }
        currentGrade = Grade.SEED;
    }

    public void reactivate(
            String loginId,
            String password,
            String name,
            String email,
            String phoneNumber,
            LocalDate birthdate,
            String ciDi
    ) {
        this.loginId = loginId;
        this.password = password;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.birthdate = birthdate;
        this.ciDi = ciDi;
        this.isActive = true;
    }
}
