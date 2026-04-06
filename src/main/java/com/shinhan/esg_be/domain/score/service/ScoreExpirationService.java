package com.shinhan.esg_be.domain.score.service;

import com.shinhan.esg_be.domain.score.entity.ExpiredScoreHistory;
import com.shinhan.esg_be.domain.score.entity.ValidScoreHistory;
import com.shinhan.esg_be.domain.score.repository.ExpiredScoreHistoryRepository;
import com.shinhan.esg_be.domain.score.repository.ValidScoreHistoryRepository;
import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScoreExpirationService {

    private final UserRepository userRepository;
    private final ValidScoreHistoryRepository validScoreHistoryRepository;
    private final ExpiredScoreHistoryRepository expiredScoreHistoryRepository;

    @Transactional
    public int expireUserScores(Long userId, LocalDateTime expiredAt) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        LocalDateTime baseDateTime = expiredAt == null ? LocalDateTime.now() : expiredAt;
        List<ValidScoreHistory> expiredScores = validScoreHistoryRepository.findByUserAndValidUntilBefore(user, baseDateTime);
        if (expiredScores.isEmpty()) {
            return 0;
        }

        for (ValidScoreHistory validScoreHistory : expiredScores) {
            // 만료된 유효 점수만큼 사용자 점수를 차감하고 만료 이력으로 옮긴다.
            user.applyScore(validScoreHistory.getCategory(), -validScoreHistory.getChangeAmount());

            expiredScoreHistoryRepository.save(
                    ExpiredScoreHistory.create(
                            user,
                            validScoreHistory.getCategory(),
                            validScoreHistory.getChangeAmount(),
                            validScoreHistory.getReason(),
                            validScoreHistory.getValidUntil(),
                            user.getScore(validScoreHistory.getCategory())
                    )
            );
        }

        validScoreHistoryRepository.deleteAll(expiredScores);
        return expiredScores.size();
    }
}
