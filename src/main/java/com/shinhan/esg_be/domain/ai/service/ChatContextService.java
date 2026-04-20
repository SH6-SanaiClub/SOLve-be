package com.shinhan.esg_be.domain.ai.service;

import com.shinhan.esg_be.domain.ai.dto.ChatUserContext;
import com.shinhan.esg_be.domain.bank.entity.enums.LoanStatus;
import com.shinhan.esg_be.domain.bank.repository.UserLoanRepository;
import com.shinhan.esg_be.domain.bank.repository.UserSavingRepository;
import com.shinhan.esg_be.domain.stat.entity.UserMonthlyStat;
import com.shinhan.esg_be.domain.stat.repository.UserMonthlyStatRepository;
import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.repository.UserRepository;
import com.shinhan.esg_be.global.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatContextService {

    private static final String KEY_PREFIX = "chat:context:";
    private static final Duration TTL = Duration.ofMinutes(10);

    private static final Map<String, String> GRADE_LABEL = Map.of(
            "SEED", "씨앗",
            "SPROUT", "새싹",
            "TREE", "나무",
            "FOREST", "숲",
            "EARTH", "지구"
    );

    private static final Map<String, Integer> NEXT_GRADE_SCORE = Map.of(
            "SEED", 600,
            "SPROUT", 700,
            "TREE", 800,
            "FOREST", 900,
            "EARTH", 1000
    );

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserRepository userRepository;
    private final UserSavingRepository userSavingRepository;
    private final UserLoanRepository userLoanRepository;
    private final UserMonthlyStatRepository userMonthlyStatRepository;

    public ChatUserContext getOrLoad(Long userId) {
        String key = KEY_PREFIX + userId;
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof ChatUserContext context) {
                log.debug("챗봇 컨텍스트 캐시 HIT userId={}", userId);
                return context;
            }
        } catch (Exception e) {
            log.warn("챗봇 컨텍스트 캐시 조회 실패 userId={}", userId, e);
        }

        log.debug("챗봇 컨텍스트 캐시 MISS - DB 조회 userId={}", userId);
        ChatUserContext context = loadFromDb(userId);
        save(userId, context);
        return context;
    }

    public void evict(Long userId) {
        try {
            redisTemplate.delete(KEY_PREFIX + userId);
            log.debug("챗봇 컨텍스트 캐시 삭제 userId={}", userId);
        } catch (Exception e) {
            log.warn("챗봇 컨텍스트 캐시 삭제 실패 userId={}", userId, e);
        }
    }

    private ChatUserContext loadFromDb(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("사용자를 찾을 수 없습니다."));

        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime nextMonthStart = monthStart.plusMonths(1);

        UserMonthlyStat currentMonthStat = userMonthlyStatRepository
                .findByUserAndMonth(userId, monthStart, nextMonthStart)
                .orElse(null);

        List<String> activeSavings = userSavingRepository.findActiveProductNamesByUserId(userId);
        boolean hasActiveLoan = userLoanRepository.existsByUser_UserIdAndStatus(userId, LoanStatus.ACTIVE);

        String gradeCode = user.getCurrentGrade().name();
        int nextScore = NEXT_GRADE_SCORE.getOrDefault(gradeCode, 1000);
        int gActivityScore = user.getGActivityScore();
        int gRepaymentScore = user.getGRepaymentScore();
        int gScore = gActivityScore + gRepaymentScore;

        return ChatUserContext.builder()
                .name(user.getName())
                .userType(user.getUserType().name())
                .totalScore(user.getTotalScore())
                .grade(GRADE_LABEL.getOrDefault(gradeCode, gradeCode))
                .eScore(user.getEScore())
                .sScore(user.getSScore())
                .gActivityScore(gActivityScore)
                .gRepaymentScore(gRepaymentScore)
                .gScore(gScore)
                .point(user.getTotalPoints())
                .monthlyEScore(currentMonthStat != null ? currentMonthStat.getMonthlyEScore() : 0)
                .monthlySScore(currentMonthStat != null ? currentMonthStat.getMonthlySScore() : 0)
                .monthlyGScore(currentMonthStat != null ? currentMonthStat.getMonthlyGScore() : 0)
                .nextGradeScore(Math.max(nextScore - user.getTotalScore(), 0))
                .activeSavings(activeSavings)
                .hasActiveLoan(hasActiveLoan)
                .loanBlocked(user.getIsLoanBlocked())
                .build();
    }

    private void save(Long userId, ChatUserContext context) {
        try {
            redisTemplate.opsForValue().set(KEY_PREFIX + userId, context, TTL);
        } catch (Exception e) {
            log.warn("챗봇 컨텍스트 캐시 저장 실패 userId={}", userId, e);
        }
    }
}
