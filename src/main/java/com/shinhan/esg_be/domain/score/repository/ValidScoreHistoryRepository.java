package com.shinhan.esg_be.domain.score.repository;

import com.shinhan.esg_be.domain.score.entity.ValidScoreHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface ValidScoreHistoryRepository extends JpaRepository<ValidScoreHistory, Long> {

    // 만료 임박 점수 존재 여부 (소프트 부스트용)
    boolean existsByUser_UserIdAndValidUntilBefore(Long userId, LocalDateTime threshold);
    boolean existsByUser_UserIdAndValidUntilBetween(Long userId, LocalDateTime from, LocalDateTime to);
}
