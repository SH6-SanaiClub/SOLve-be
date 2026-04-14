package com.shinhan.esg_be.domain.volunteer.scheduler;

import com.shinhan.esg_be.domain.volunteer.service.VolunteerStatusTransitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.scheduler.enabled", havingValue = "true")
public class VolunteerStatusScheduler {

    private final VolunteerStatusTransitionService volunteerStatusTransitionService;

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    public void processVolunteerStatuses() {
        volunteerStatusTransitionService.processDueTransitions();
    }
}
