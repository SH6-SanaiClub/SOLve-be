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
import java.util.stream.Collectors;

import static com.shinhan.esg_be.global.common.enums.ScoreReason.ABUSE;
import static com.shinhan.esg_be.global.common.enums.ScoreReason.INITIAL_SCORE;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScoreExpirationService {

    private final UserRepository userRepository;
    private final ValidScoreHistoryRepository validScoreHistoryRepository;
    private final ExpiredScoreHistoryRepository expiredScoreHistoryRepository;
    private final ScoreRecalculationService scoreRecalculationService;

    @Transactional
    public int expireUserScores(Long userId, LocalDateTime expiredAt) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        LocalDateTime baseDateTime = expiredAt == null ? LocalDateTime.now() : expiredAt;
        List<ValidScoreHistory> expiredScores = validScoreHistoryRepository.findByUserAndValidUntilBefore(user, baseDateTime)
                .stream()
                .filter(validScoreHistory -> validScoreHistory.getReason() != INITIAL_SCORE)
                .filter(validScoreHistory -> validScoreHistory.getReason() != ABUSE)
                .collect(Collectors.toList());
        if (expiredScores.isEmpty()) {
            return 0;
        }

        for (ValidScoreHistory validScoreHistory : expiredScores) {
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
        validScoreHistoryRepository.flush();
        scoreRecalculationService.recalculateUserScore(user);
        return expiredScores.size();
    }
}
