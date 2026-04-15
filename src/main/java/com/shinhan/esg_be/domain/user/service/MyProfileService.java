package com.shinhan.esg_be.domain.user.service;

import com.shinhan.esg_be.domain.bank.entity.enums.LoanStatus;
import com.shinhan.esg_be.domain.bank.entity.enums.SavingStatus;
import com.shinhan.esg_be.domain.bank.repository.UserLoanRepository;
import com.shinhan.esg_be.domain.bank.repository.UserSavingRepository;
import com.shinhan.esg_be.domain.auth.dto.internal.VerifiedIdentityDetails;
import com.shinhan.esg_be.domain.auth.service.PortOneIdentityVerificationService;
import com.shinhan.esg_be.domain.user.dto.response.CheckMyPasswordResponse;
import com.shinhan.esg_be.domain.user.dto.response.MyProfileResponse;
import com.shinhan.esg_be.domain.user.dto.response.UpdateMyEmailResponse;
import com.shinhan.esg_be.domain.user.dto.response.UpdateMyPasswordResponse;
import com.shinhan.esg_be.domain.user.dto.response.UpdateMyPhoneNumberResponse;
import com.shinhan.esg_be.domain.user.dto.response.WithdrawMyAccountResponse;
import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.repository.UserRepository;
import com.shinhan.esg_be.global.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
// 마이페이지 프로필 조회 및 수정 로직 처리
public class MyProfileService {

    private final UserRepository userRepository;
    private final UserSavingRepository userSavingRepository;
    private final UserLoanRepository userLoanRepository;
    private final PortOneIdentityVerificationService portOneIdentityVerificationService;
    private final BCryptPasswordEncoder passwordEncoder;

    public MyProfileResponse getMyProfile(Long userId) {
        return MyProfileResponse.from(findUser(userId));
    }

    // 현재 비밀번호 일치 여부 확인
    public CheckMyPasswordResponse checkMyPassword(Long userId, String currentPassword) {
        User user = findUser(userId);
        boolean matched = passwordEncoder.matches(currentPassword, user.getPassword());

        return new CheckMyPasswordResponse(
                matched,
                matched ? "현재 비밀번호가 일치합니다." : "현재 비밀번호가 일치하지 않습니다."
        );
    }

    @Transactional
    // 현재 비밀번호 확인 후 새 비밀번호로 변경
    public UpdateMyPasswordResponse updateMyPassword(
            Long userId,
            String currentPassword,
            String newPassword,
            String newPasswordConfirm
    ) {
        User user = findUser(userId);

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BadRequestException("현재 비밀번호가 일치하지 않습니다.");
        }

        if (!newPassword.equals(newPasswordConfirm)) {
            throw new BadRequestException("새 비밀번호가 일치하지 않습니다.");
        }

        user.updatePassword(passwordEncoder.encode(newPassword));

        return new UpdateMyPasswordResponse("비밀번호가 변경되었습니다.");
    }

    @Transactional
    // 중복 확인 후 이메일 변경
    public UpdateMyEmailResponse updateMyEmail(Long userId, String email) {
        User user = findUser(userId);
        String trimmedEmail = email == null ? "" : email.trim();

        if (trimmedEmail.equals(user.getEmail())) {
            return new UpdateMyEmailResponse(
                    user.getEmail(),
                    "이메일이 변경되었습니다."
            );
        }

        if (userRepository.existsByEmail(trimmedEmail)) {
            throw new BadRequestException("이미 사용 중인 이메일입니다.");
        }

        user.updateEmail(trimmedEmail);

        return new UpdateMyEmailResponse(
                user.getEmail(),
                "이메일이 변경되었습니다."
        );
    }

    @Transactional
    // 포트원 본인인증 결과로 휴대폰 번호와 ciDi 변경
    public UpdateMyPhoneNumberResponse updateMyPhoneNumber(Long userId, String impUid) {
        User user = findUser(userId);
        VerifiedIdentityDetails verifiedIdentityDetails =
                portOneIdentityVerificationService.verifyDetails(impUid);

        String formattedPhoneNumber = formatPhoneNumber(verifiedIdentityDetails.getPhoneNumber());

        if (userRepository.existsByPhoneNumberAndUserIdNot(formattedPhoneNumber, userId)) {
            throw new BadRequestException("이미 등록된 전화번호입니다.");
        }

        if (userRepository.existsByCiDiAndUserIdNot(verifiedIdentityDetails.getCiDi(), userId)) {
            throw new BadRequestException("이미 가입된 본인 인증 정보입니다.");
        }

        user.updatePhoneNumberAndCiDi(formattedPhoneNumber, verifiedIdentityDetails.getCiDi());

        return new UpdateMyPhoneNumberResponse(
                user.getPhoneNumber(),
                "휴대폰 번호가 변경되었습니다."
        );
    }

    @Transactional
    // 활성 대출·적금이 없을 때만 회원탈퇴 처리
    public WithdrawMyAccountResponse withdrawMyAccount(Long userId) {
        User user = findUser(userId);

        if (userSavingRepository.existsByUser_UserIdAndStatus(userId, SavingStatus.ACTIVE)) {
            throw new BadRequestException("적금 상품을 보유하고 있어 회원탈퇴가 불가능합니다.");
        }

        if (userLoanRepository.existsByUser_UserIdAndStatus(userId, LoanStatus.ACTIVE)) {
            throw new BadRequestException("대출 상품을 보유하고 있어 회원탈퇴가 불가능합니다.");
        }

        user.withdraw();

        return new WithdrawMyAccountResponse("회원탈퇴가 완료되었습니다.");
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("사용자를 찾을 수 없습니다."));
    }

    private String formatPhoneNumber(String phoneNumber) {
        String digitsOnly = phoneNumber == null ? "" : phoneNumber.replaceAll("[^0-9]", "");

        if (digitsOnly.length() != 11) {
            throw new BadRequestException("본인인증 결과에서 유효한 휴대폰 번호를 찾을 수 없습니다.");
        }

        return digitsOnly.substring(0, 3)
                + "-"
                + digitsOnly.substring(3, 7)
                + "-"
                + digitsOnly.substring(7);
    }
}
