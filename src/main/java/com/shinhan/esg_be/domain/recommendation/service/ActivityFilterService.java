package com.shinhan.esg_be.domain.recommendation.service;

import com.shinhan.esg_be.domain.recommendation.dto.ActivityCandidateDto;
import com.shinhan.esg_be.domain.recommendation.dto.UserFeatureDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityFilterService {

    private final ActivityAvailabilityService activityAvailabilityService;

    public List<ActivityCandidateDto> filter(
            List<ActivityCandidateDto> candidates,
            UserFeatureDto feature
    ) {
        return filterInternal(candidates, feature, true);
    }

    public List<ActivityCandidateDto> filterIgnoringMonthlyLimit(
            List<ActivityCandidateDto> candidates,
            UserFeatureDto feature
    ) {
        return filterInternal(candidates, feature, false);
    }

    private List<ActivityCandidateDto> filterInternal(
            List<ActivityCandidateDto> candidates,
            UserFeatureDto feature,
            boolean enforceMonthlyLimit
    ) {
        return candidates.stream()
                .filter(c -> passesAllFilters(c, feature, enforceMonthlyLimit))
                .collect(Collectors.toList());
    }

    private boolean passesAllFilters(
            ActivityCandidateDto c,
            UserFeatureDto feature,
            boolean enforceMonthlyLimit
    ) {
        ActivityAvailabilityStatus status = activityAvailabilityService.evaluate(c, feature, enforceMonthlyLimit);
        if (!status.canParticipate()) {
            log.debug(
                    "추천 필터 탈락 - activityType={} referenceId={} blockedReason={}",
                    c.getActivityType(),
                    c.getReferenceId(),
                    status.blockedReasonCode()
            );
        }
        return status.canParticipate();
    }
}
