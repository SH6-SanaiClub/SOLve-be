package com.shinhan.esg_be.domain.score.repository;

import com.shinhan.esg_be.domain.score.entity.ExpiredScoreHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpiredScoreHistoryRepository extends JpaRepository<ExpiredScoreHistory, Long> {
}
