package com.shinhan.esg_be.domain.stat.entity;

import com.shinhan.esg_be.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(
        name = "user_score_snapshot",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_score_snapshot_user_date",
                columnNames = {"user_id", "snapshot_date"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserScoreSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "snapshot_id")
    private Long snapshotId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "total_score", nullable = false)
    private Integer totalScore;

    public static UserScoreSnapshot create(User user, LocalDate snapshotDate) {
        UserScoreSnapshot snapshot = new UserScoreSnapshot();
        snapshot.user = user;
        snapshot.snapshotDate = snapshotDate;
        snapshot.updateScores(user);
        return snapshot;
    }

    public void updateScores(User user) {
        this.totalScore = user.getTotalScore();
    }
}
