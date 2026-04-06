package com.shinhan.esg_be.domain.score.repository;

import com.shinhan.esg_be.domain.score.entity.ValidScoreHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ValidScoreHistoryRepository extends JpaRepository<ValidScoreHistory, Long> {

    List<ValidScoreHistory> findByValidUntilBefore(LocalDateTime validUntil);
}
