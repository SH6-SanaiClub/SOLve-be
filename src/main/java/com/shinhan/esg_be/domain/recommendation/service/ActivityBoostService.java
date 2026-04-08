package com.shinhan.esg_be.domain.recommendation.service;

import com.shinhan.esg_be.domain.recommendation.dto.ActivityCandidateDto;
import com.shinhan.esg_be.domain.recommendation.dto.UserFeatureDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
public class ActivityBoostService {

    private static final double BOOST_INACTIVITY_RISK = 0.15; // 미활동 패널티 위험
    private static final double BOOST_DEADLINE_NEAR = 0.10; // 마감 7일 이내
    private static final double BOOST_SCORE_EXPIRY = 0.10; // 유효점수 만료 임박
    private static final double BOOST_MAX = 0.25; // 부스트 합산 최대

    public List<ActivityCandidateDto> applyBoost(
            List<ActivityCandidateDto> candidates,
            UserFeatureDto feature
    ) {
        for (ActivityCandidateDto c : candidates) {
            double boost = 0.0;

            // 부스트 1: 미활동 패널티 위험 (최근 30일 활동 없음)
            if (feature.isInactivityRisk()) {
                boost += BOOST_INACTIVITY_RISK;
                log.debug("부스트 적용 - 미활동위험 refId={}", c.getReferenceId());
            }

            // 부스트 2: 마감 7일 이내 (기부/봉사)
            if (isDeadlineNear(c)) {
                boost += BOOST_DEADLINE_NEAR;
                log.debug("부스트 적용 - 마감임박 refId={}", c.getReferenceId());
            }

            // 부스트 3: 유효점수 만료 임박
            if (feature.isScoreExpiryRisk()) {
                boost += BOOST_SCORE_EXPIRY;
                log.debug("부스트 적용 - 점수만료임박 refId={}", c.getReferenceId());
            }

            // 합산 캡 적용
            boost = Math.min(boost, BOOST_MAX);

            // finalScore에 부스트 반영
            double boostedScore = c.getFinalScore() * (1 + boost);
            c.setBoostValue(boost);
            c.setFinalScore(boostedScore);

            log.debug("boost 완료 - type={} refId={} boost={} finalScore={}",
                    c.getActivityType(), c.getReferenceId(), boost, boostedScore);
        }

        return candidates;
    }

    // 마감 7일 이내 여부 (기부 end_date, 봉사 activity_date 기준)
    private boolean isDeadlineNear(ActivityCandidateDto c) {
        if (c.getDeadlineDate() == null) {
            return false;
        }
        if (!"DONATION".equals(c.getActivityType()) && !"VOLUNTEER".equals(c.getActivityType())) {
            return false;
        }

        long daysUntilDeadline = ChronoUnit.DAYS.between(LocalDate.now(), c.getDeadlineDate());
        return daysUntilDeadline >= 0 && daysUntilDeadline <= 7;
    }
}
