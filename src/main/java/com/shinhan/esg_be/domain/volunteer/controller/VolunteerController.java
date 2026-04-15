package com.shinhan.esg_be.domain.volunteer.controller;

import com.shinhan.esg_be.domain.volunteer.dto.request.VolunteerApplyRequest;
import com.shinhan.esg_be.domain.volunteer.dto.request.VolunteerCheckInRequest;
import com.shinhan.esg_be.domain.volunteer.dto.request.VolunteerCheckOutRequest;
import com.shinhan.esg_be.domain.volunteer.dto.response.VolunteerApplyResponse;
import com.shinhan.esg_be.domain.volunteer.dto.response.VolunteerApplicationResponse;
import com.shinhan.esg_be.domain.volunteer.dto.response.VolunteerAttendanceResponse;
import com.shinhan.esg_be.domain.volunteer.dto.response.VolunteerCheckInResponse;
import com.shinhan.esg_be.domain.volunteer.dto.response.VolunteerCheckOutResponse;
import com.shinhan.esg_be.domain.volunteer.dto.response.VolunteerDetailResponse;
import com.shinhan.esg_be.domain.volunteer.dto.response.VolunteerResponse;
import com.shinhan.esg_be.domain.volunteer.service.VolunteerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.DeleteMapping;
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

    @GetMapping("/volunteers/applications")
    public ResponseEntity<VolunteerApplicationResponse> getVolunteerApplications() {
        return ResponseEntity.ok(volunteerService.getMyVolunteerApplications());
    }

    @GetMapping("/volunteers/{volunteerId}")
    public ResponseEntity<VolunteerDetailResponse> getVolunteer(@PathVariable Long volunteerId) {
        return ResponseEntity.ok(volunteerService.getVolunteer(volunteerId));
    }

    @PostMapping("/volunteers/apply")
    public ResponseEntity<VolunteerApplyResponse> applyVolunteer(@RequestBody @Valid VolunteerApplyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(volunteerService.applyVolunteer(request));
    }

    @DeleteMapping("/volunteers/applications/{volunteerApplicationId}")
    public ResponseEntity<Void> cancelVolunteerApplication(@PathVariable Long volunteerApplicationId) {
        volunteerService.cancelVolunteerApplication(volunteerApplicationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/volunteers/attendance")
    public ResponseEntity<VolunteerAttendanceResponse> getVolunteerAttendance(@RequestParam String token) {
        return ResponseEntity.ok(volunteerService.getVolunteerAttendance(token));
    }

    @PostMapping("/volunteers/check-in")
    public ResponseEntity<VolunteerCheckInResponse> checkInVolunteer(
            @RequestBody @Valid VolunteerCheckInRequest request
    ) {
        return ResponseEntity.ok(volunteerService.checkInVolunteer(request));
    }

    @PostMapping("/volunteers/check-out")
    public ResponseEntity<VolunteerCheckOutResponse> checkOutVolunteer(
            @RequestBody @Valid VolunteerCheckOutRequest request
    ) {
        return ResponseEntity.ok(volunteerService.checkOutVolunteer(request));
    }
}
