package com.shinhan.esg_be.domain.user.controller;

import com.shinhan.esg_be.domain.user.dto.request.CheckMyPasswordRequest;
import com.shinhan.esg_be.domain.user.dto.request.UpdateMyEmailRequest;
import com.shinhan.esg_be.domain.user.dto.request.UpdateMyPasswordRequest;
import com.shinhan.esg_be.domain.user.dto.request.UpdateMyPhoneNumberRequest;
import com.shinhan.esg_be.domain.user.dto.response.CheckMyPasswordResponse;
import com.shinhan.esg_be.domain.user.dto.response.MyProfileResponse;
import com.shinhan.esg_be.domain.user.dto.response.UpdateMyEmailResponse;
import com.shinhan.esg_be.domain.user.dto.response.UpdateMyPasswordResponse;
import com.shinhan.esg_be.domain.user.dto.response.UpdateMyPhoneNumberResponse;
import com.shinhan.esg_be.domain.user.dto.response.WithdrawMyAccountResponse;
import com.shinhan.esg_be.domain.user.service.MyProfileService;
import com.shinhan.esg_be.global.security.AuthContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/my/profile")
@RequiredArgsConstructor
// 마이페이지 프로필 조회 및 수정 API 제공
public class MyProfileController {

    private final MyProfileService myProfileService;
    private final AuthContext authContext;

    @GetMapping
    public ResponseEntity<MyProfileResponse> getMyProfile() {
        return ResponseEntity.ok(myProfileService.getMyProfile(authContext.currentUserId()));
    }

    @PostMapping("/password/check")
    public ResponseEntity<CheckMyPasswordResponse> checkMyPassword(
            @RequestBody @Valid CheckMyPasswordRequest request
    ) {
        return ResponseEntity.ok(
                myProfileService.checkMyPassword(
                        authContext.currentUserId(),
                        request.getCurrentPassword()
                )
        );
    }

    @PatchMapping("/password")
    public ResponseEntity<UpdateMyPasswordResponse> updateMyPassword(
            @RequestBody @Valid UpdateMyPasswordRequest request
    ) {
        return ResponseEntity.ok(
                myProfileService.updateMyPassword(
                        authContext.currentUserId(),
                        request.getCurrentPassword(),
                        request.getNewPassword(),
                        request.getNewPasswordConfirm()
                )
        );
    }

    @PatchMapping("/email")
    public ResponseEntity<UpdateMyEmailResponse> updateMyEmail(
            @RequestBody @Valid UpdateMyEmailRequest request
    ) {
        return ResponseEntity.ok(
                myProfileService.updateMyEmail(authContext.currentUserId(), request.getEmail())
        );
    }

    @PatchMapping("/phone")
    public ResponseEntity<UpdateMyPhoneNumberResponse> updateMyPhoneNumber(
            @RequestBody @Valid UpdateMyPhoneNumberRequest request
    ) {
        return ResponseEntity.ok(
                myProfileService.updateMyPhoneNumber(authContext.currentUserId(), request.getImpUid())
        );
    }

    @PatchMapping("/withdraw")
    public ResponseEntity<WithdrawMyAccountResponse> withdrawMyAccount() {
        return ResponseEntity.ok(myProfileService.withdrawMyAccount(authContext.currentUserId()));
    }
}
