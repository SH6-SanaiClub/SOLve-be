package com.shinhan.esg_be.domain.score.entity;

import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.global.common.enums.ActivityType;
import com.shinhan.esg_be.global.common.enums.ScoreCategory;
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
@Table(name = "expired_score_history")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpiredScoreHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "score_id")
    private Long scoreId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScoreCategory category;

    @Column(name = "change_amount", nullable = false)
    private Integer changeAmount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ActivityType reason;

    @Column(name = "valid_until", nullable = false)
    private LocalDateTime validUntil;

    @Column(name = "score_after", nullable = false)
    private Integer scoreAfter;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static ExpiredScoreHistory create(
            User user,
            ScoreCategory category,
            int changeAmount,
            ActivityType reason,
            LocalDateTime validUntil,
            int scoreAfter
    ) {
        ExpiredScoreHistory expiredScoreHistory = new ExpiredScoreHistory();
        expiredScoreHistory.user = user;
        expiredScoreHistory.category = category;
        expiredScoreHistory.changeAmount = changeAmount;
        expiredScoreHistory.reason = reason;
        expiredScoreHistory.validUntil = validUntil;
        expiredScoreHistory.scoreAfter = scoreAfter;
        return expiredScoreHistory;
    }
}
