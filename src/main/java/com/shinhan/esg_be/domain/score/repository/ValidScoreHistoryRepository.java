package com.shinhan.esg_be.domain.score.repository;

import com.shinhan.esg_be.domain.score.entity.ValidScoreHistory;
import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.global.common.enums.ScoreReason;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ValidScoreHistoryRepository extends JpaRepository<ValidScoreHistory, Long> {

    List<ValidScoreHistory> findByValidUntilBefore(LocalDateTime validUntil);

    List<ValidScoreHistory> findByUserAndValidUntilBefore(User user, LocalDateTime validUntil);

    boolean existsByUserAndReason(User user, ScoreReason reason);
}
