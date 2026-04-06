package com.shinhan.esg_be.domain.stat.entity;

import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.global.common.enums.ScoreCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_monthly_stat")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserMonthlyStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "monthly_stat_id")
    private Long monthlyStatId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "monthly_e_score", nullable = false)
    private Integer monthlyEScore = 0;

    @Column(name = "monthly_s_score", nullable = false)
    private Integer monthlySScore = 0;

    @Column(name = "monthly_g_score", nullable = false)
    private Integer monthlyGScore = 0;

    @Column(name = "consecutive_e_max_score", nullable = false)
    private Integer consecutiveEMaxScore = 0;

    @Column(name = "consecutive_s_max_score", nullable = false)
    private Integer consecutiveSMaxScore = 0;

    @Column(name = "consecutive_g_max_score", nullable = false)
    private Integer consecutiveGMaxScore = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static UserMonthlyStat create(User user) {
        UserMonthlyStat userMonthlyStat = new UserMonthlyStat();
        userMonthlyStat.user = user;
        return userMonthlyStat;
    }

    public void addScore(ScoreCategory scoreCategory, int scoreDelta) {
        if (scoreDelta == 0) {
            return;
        }

        switch (scoreCategory) {
            case E -> monthlyEScore += scoreDelta;
            case S -> monthlySScore += scoreDelta;
            case G_ACTIVITY -> monthlyGScore += scoreDelta;
            case G_REPAYMENT -> {
            }
        }
    }

    public int getMonthlyScore(ScoreCategory scoreCategory) {
        return switch (scoreCategory) {
            case E -> monthlyEScore;
            case S -> monthlySScore;
            case G_ACTIVITY, G_REPAYMENT -> monthlyGScore;
        };
    }

    public void updateEConsecutiveMaxScore(int consecutiveCount) {
        this.consecutiveEMaxScore = consecutiveCount;
    }

    public void updateSConsecutiveMaxScore(int consecutiveCount) {
        this.consecutiveSMaxScore = consecutiveCount;
    }

    public void updateGConsecutiveMaxScore(int consecutiveCount) {
        this.consecutiveGMaxScore = consecutiveCount;
    }

    public void resetMonthlyScores() {
        monthlyEScore = 0;
        monthlySScore = 0;
        monthlyGScore = 0;
    }
}
