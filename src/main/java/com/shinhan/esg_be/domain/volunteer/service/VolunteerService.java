package com.shinhan.esg_be.domain.volunteer.service;

import com.shinhan.esg_be.domain.volunteer.dto.response.VolunteerItemResponse;
import com.shinhan.esg_be.domain.volunteer.dto.response.VolunteerResponse;
import com.shinhan.esg_be.domain.volunteer.repository.VolunteerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VolunteerService {

    private final VolunteerRepository volunteerRepository;

    public VolunteerResponse getVolunteers() {
        return new VolunteerResponse(
                volunteerRepository.findActiveVolunteerList(LocalDateTime.now())
        );
    }
}
