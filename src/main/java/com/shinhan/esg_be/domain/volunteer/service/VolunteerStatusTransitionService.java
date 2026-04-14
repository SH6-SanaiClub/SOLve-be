package com.shinhan.esg_be.domain.volunteer.service;

import com.shinhan.esg_be.domain.volunteer.entity.UserVolunteer;
import com.shinhan.esg_be.domain.volunteer.entity.enums.VolunteerStatus;
import com.shinhan.esg_be.domain.volunteer.repository.UserVolunteerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class VolunteerStatusTransitionService {

    private static final String TRANSITION_KEY = "volunteer:status:transitions";
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserVolunteerRepository userVolunteerRepository;

    public void scheduleTransition(UserVolunteer userVolunteer, LocalDateTime transitionAt) {
        redisTemplate.opsForZSet().add(
                TRANSITION_KEY,
                String.valueOf(userVolunteer.getVolunteerApplicationsId()),
                toEpochMillis(transitionAt)
        );
    }

    public void clearTransition(Long volunteerApplicationId) {
        redisTemplate.opsForZSet().remove(TRANSITION_KEY, String.valueOf(volunteerApplicationId));
    }

    @Transactional
    public int processDueTransitions() {
        long nowScore = toEpochMillis(LocalDateTime.now());
        Set<Object> dueMembers = redisTemplate.opsForZSet()
                .rangeByScore(TRANSITION_KEY, 0, nowScore);

        if (dueMembers == null || dueMembers.isEmpty()) {
            return 0;
        }

        List<Long> ids = dueMembers.stream()
                .map(Object::toString)
                .map(Long::valueOf)
                .toList();

        List<UserVolunteer> targets = userVolunteerRepository.findAllByVolunteerApplicationsIdIn(ids);
        int updatedCount = 0;

        for (UserVolunteer userVolunteer : targets) {
            if (userVolunteer.getStatus() == VolunteerStatus.APPLIED) {
                userVolunteer.markNoShow();
                updatedCount++;
            } else if (userVolunteer.getStatus() == VolunteerStatus.ATTENDED
                    && userVolunteer.getCheckOutAt() == null) {
                userVolunteer.markIncomplete();
                updatedCount++;
            }
            clearTransition(userVolunteer.getVolunteerApplicationsId());
        }

        log.info("봉사 상태 자동 전환 완료 count={}", updatedCount);
        return updatedCount;
    }

    private long toEpochMillis(LocalDateTime dateTime) {
        return dateTime.atZone(ZONE_ID).toInstant().toEpochMilli();
    }
}
