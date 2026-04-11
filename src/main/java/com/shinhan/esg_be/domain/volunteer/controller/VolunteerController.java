package com.shinhan.esg_be.domain.volunteer.controller;

import com.shinhan.esg_be.domain.volunteer.dto.response.VolunteerResponse;
import com.shinhan.esg_be.domain.volunteer.service.VolunteerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/esg/s")
@RequiredArgsConstructor
public class VolunteerController {

    private final VolunteerService volunteerService;

    @GetMapping("/volunteers")
    public ResponseEntity<VolunteerResponse> getVolunteers() {
        return ResponseEntity.ok(volunteerService.getVolunteers());
    }
}
