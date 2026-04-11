package com.shinhan.esg_be.domain.volunteer.service;

import com.shinhan.esg_be.domain.volunteer.dto.response.VolunteerItemResponse;
import com.shinhan.esg_be.domain.volunteer.dto.response.VolunteerDetailResponse;
import com.shinhan.esg_be.domain.volunteer.dto.response.VolunteerResponse;
import com.shinhan.esg_be.domain.volunteer.repository.VolunteerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

import static org.springframework.http.HttpStatus.NOT_FOUND;

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

    public VolunteerDetailResponse getVolunteer(Long volunteerId) {
        return volunteerRepository.findActiveVolunteerDetail(volunteerId, LocalDateTime.now())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "봉사활동을 찾을 수 없습니다."));
    }
}
