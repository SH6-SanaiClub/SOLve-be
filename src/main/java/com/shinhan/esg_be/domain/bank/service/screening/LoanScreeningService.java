package com.shinhan.esg_be.domain.bank.service.screening;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanScreeningService {

    private static final String KEY_PREFIX = "loan:screening:";
    private static final Duration TTL = Duration.ofHours(24);

    private final RedisTemplate<String, Object> redisTemplate;
    private final MockVirtualCbClient mockVirtualCbClient;
    private final LoanScreeningPolicy loanScreeningPolicy;

    public LoanScreeningResult screen(
            Long userId,
            int esgScore,
            boolean hasActiveLoan,
            boolean loanBlocked
    ) {
        if (loanBlocked || hasActiveLoan) {
            return loanScreeningPolicy.evaluate(esgScore, 0, false, hasActiveLoan, loanBlocked);
        }

        VirtualCbAssessmentResult assessment = getOrCreateAssessment(userId);
        LoanScreeningResult result = loanScreeningPolicy.evaluate(
                esgScore,
                assessment.cbScore(),
                assessment.hasTelecomDelinquency(),
                false,
                false
        );

        log.info(
                "Loan screening completed: userId={}, cbScore={}, telecomDelinquency={}, approved={}, reason={}",
                userId,
                assessment.cbScore(),
                assessment.hasTelecomDelinquency(),
                result.approved(),
                result.reason()
        );

        return result;
    }

    private VirtualCbAssessmentResult getOrCreateAssessment(Long userId) {
        String key = KEY_PREFIX + userId;

        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof VirtualCbAssessmentResult result) {
                log.debug("Loan screening cache HIT userId={}", userId);
                return result;
            }
        } catch (Exception e) {
            log.warn("Loan screening cache read failed userId={}", userId, e);
        }

        VirtualCbAssessmentResult assessment = mockVirtualCbClient.assess(userId);

        try {
            redisTemplate.opsForValue().set(key, assessment, TTL);
            log.debug("Loan screening cache saved userId={}", userId);
        } catch (Exception e) {
            log.warn("Loan screening cache write failed userId={}", userId, e);
        }

        return assessment;
    }
}
