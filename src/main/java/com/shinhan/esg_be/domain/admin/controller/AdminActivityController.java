package com.shinhan.esg_be.domain.admin.controller;

import com.shinhan.esg_be.domain.admin.dto.request.AdminActivityCreateRequest;
import com.shinhan.esg_be.domain.admin.dto.request.AdminActivityUpdateRequest;
import com.shinhan.esg_be.domain.admin.dto.request.AdminDonationCreateRequest;
import com.shinhan.esg_be.domain.admin.dto.request.AdminDonationUpdateRequest;
import com.shinhan.esg_be.domain.admin.dto.request.AdminEcoProductCreateRequest;
import com.shinhan.esg_be.domain.admin.dto.request.AdminEcoProductUpdateRequest;
import com.shinhan.esg_be.domain.admin.dto.request.AdminStatusUpdateRequest;
import com.shinhan.esg_be.domain.admin.dto.request.AdminVolunteerCreateRequest;
import com.shinhan.esg_be.domain.admin.dto.request.AdminVolunteerUpdateRequest;
import com.shinhan.esg_be.domain.admin.dto.response.AdminActivityResponse;
import com.shinhan.esg_be.domain.admin.dto.response.AdminDonationResponse;
import com.shinhan.esg_be.domain.admin.dto.response.AdminEcoProductResponse;
import com.shinhan.esg_be.domain.admin.dto.response.AdminVolunteerResponse;
import com.shinhan.esg_be.domain.admin.service.AdminActivityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminActivityController {

    private final AdminActivityService adminActivityService;

    @GetMapping("/activities")
    public ResponseEntity<List<AdminActivityResponse>> getActivities() {
        return ResponseEntity.ok(adminActivityService.getActivities());
    }

    @PostMapping("/activities")
    public ResponseEntity<AdminActivityResponse> createActivity(
            @RequestBody @Valid AdminActivityCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminActivityService.createActivity(request));
    }

    @PutMapping("/activities/{activityId}")
    public ResponseEntity<AdminActivityResponse> updateActivity(
            @PathVariable Long activityId,
            @RequestBody @Valid AdminActivityUpdateRequest request
    ) {
        return ResponseEntity.ok(adminActivityService.updateActivity(activityId, request));
    }

    @PatchMapping("/activities/{activityId}/status")
    public ResponseEntity<AdminActivityResponse> updateActivityStatus(
            @PathVariable Long activityId,
            @RequestBody @Valid AdminStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(adminActivityService.updateActivityStatus(activityId, request));
    }

    @GetMapping("/donations")
    public ResponseEntity<List<AdminDonationResponse>> getDonations() {
        return ResponseEntity.ok(adminActivityService.getDonations());
    }

    @GetMapping("/donations/{donationId}")
    public ResponseEntity<AdminDonationResponse> getDonation(@PathVariable Long donationId) {
        return ResponseEntity.ok(adminActivityService.getDonation(donationId));
    }

    @PostMapping("/donations")
    public ResponseEntity<AdminDonationResponse> createDonation(
            @RequestBody @Valid AdminDonationCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminActivityService.createDonation(request));
    }

    @PutMapping("/donations/{donationId}")
    public ResponseEntity<AdminDonationResponse> updateDonation(
            @PathVariable Long donationId,
            @RequestBody @Valid AdminDonationUpdateRequest request
    ) {
        return ResponseEntity.ok(adminActivityService.updateDonation(donationId, request));
    }

    @PatchMapping("/donations/{donationId}/status")
    public ResponseEntity<AdminDonationResponse> updateDonationStatus(
            @PathVariable Long donationId,
            @RequestBody @Valid AdminStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(adminActivityService.updateDonationStatus(donationId, request));
    }

    @DeleteMapping("/donations/{donationId}")
    public ResponseEntity<Void> deleteDonation(@PathVariable Long donationId) {
        adminActivityService.deleteDonation(donationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/volunteers")
    public ResponseEntity<List<AdminVolunteerResponse>> getVolunteers() {
        return ResponseEntity.ok(adminActivityService.getVolunteers());
    }

    @GetMapping("/volunteers/{volunteerId}")
    public ResponseEntity<AdminVolunteerResponse> getVolunteer(@PathVariable Long volunteerId) {
        return ResponseEntity.ok(adminActivityService.getVolunteer(volunteerId));
    }

    @PostMapping("/volunteers")
    public ResponseEntity<AdminVolunteerResponse> createVolunteer(
            @RequestBody @Valid AdminVolunteerCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminActivityService.createVolunteer(request));
    }

    @PutMapping("/volunteers/{volunteerId}")
    public ResponseEntity<AdminVolunteerResponse> updateVolunteer(
            @PathVariable Long volunteerId,
            @RequestBody @Valid AdminVolunteerUpdateRequest request
    ) {
        return ResponseEntity.ok(adminActivityService.updateVolunteer(volunteerId, request));
    }

    @PatchMapping("/volunteers/{volunteerId}/status")
    public ResponseEntity<AdminVolunteerResponse> updateVolunteerStatus(
            @PathVariable Long volunteerId,
            @RequestBody @Valid AdminStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(adminActivityService.updateVolunteerStatus(volunteerId, request));
    }

    @DeleteMapping("/volunteers/{volunteerId}")
    public ResponseEntity<Void> deleteVolunteer(@PathVariable Long volunteerId) {
        adminActivityService.deleteVolunteer(volunteerId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/eco-products")
    public ResponseEntity<List<AdminEcoProductResponse>> getEcoProducts() {
        return ResponseEntity.ok(adminActivityService.getEcoProducts());
    }

    @PostMapping("/eco-products")
    public ResponseEntity<AdminEcoProductResponse> createEcoProduct(
            @RequestBody @Valid AdminEcoProductCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminActivityService.createEcoProduct(request));
    }

    @PutMapping("/eco-products/{productId}")
    public ResponseEntity<AdminEcoProductResponse> updateEcoProduct(
            @PathVariable Long productId,
            @RequestBody @Valid AdminEcoProductUpdateRequest request
    ) {
        return ResponseEntity.ok(adminActivityService.updateEcoProduct(productId, request));
    }

    @PatchMapping("/eco-products/{productId}/status")
    public ResponseEntity<AdminEcoProductResponse> updateEcoProductStatus(
            @PathVariable Long productId,
            @RequestBody @Valid AdminStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(adminActivityService.updateEcoProductStatus(productId, request));
    }

    @DeleteMapping("/eco-products/{productId}")
    public ResponseEntity<Void> deleteEcoProduct(@PathVariable Long productId) {
        adminActivityService.deleteEcoProduct(productId);
        return ResponseEntity.noContent().build();
    }
}
