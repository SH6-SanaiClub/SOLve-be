package com.shinhan.esg_be.domain.score.repository;

import com.shinhan.esg_be.domain.score.entity.ValidScoreHistory;
import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.global.common.enums.ScoreReason;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ValidScoreHistoryRepository extends JpaRepository<ValidScoreHistory, Long> {

    List<ValidScoreHistory> findByValidUntilBefore(LocalDateTime validUntil);

    List<ValidScoreHistory> findByUserAndValidUntilBefore(User user, LocalDateTime validUntil);

    boolean existsByUserAndReason(User user, ScoreReason reason);

    boolean existsByUserAndReasonAndCreatedAtBetween(
            User user,
            ScoreReason reason,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );

    List<ValidScoreHistory> findByUserAndReasonIn(User user, Collection<ScoreReason> reasons);
  
    // 만료 임박 점수 존재 여부 (소프트 부스트용)
    boolean existsByUser_UserIdAndValidUntilBefore(Long userId, LocalDateTime threshold);
  
    boolean existsByUser_UserIdAndValidUntilBetween(Long userId, LocalDateTime from, LocalDateTime to);
}
