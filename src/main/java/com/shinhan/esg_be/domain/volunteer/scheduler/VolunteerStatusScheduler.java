package com.shinhan.esg_be.domain.volunteer.scheduler;

import com.shinhan.esg_be.domain.volunteer.service.VolunteerStatusTransitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VolunteerStatusScheduler {

    private final VolunteerStatusTransitionService volunteerStatusTransitionService;

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    public void processVolunteerStatuses() {
        volunteerStatusTransitionService.processDueTransitions();
    }
}
