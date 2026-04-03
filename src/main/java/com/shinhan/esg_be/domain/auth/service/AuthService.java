package com.shinhan.esg_be.domain.auth.service;

import com.shinhan.esg_be.domain.auth.dto.request.AuthJoinRequest;
import com.shinhan.esg_be.domain.auth.dto.request.AuthLoginRequest;
import com.shinhan.esg_be.domain.auth.dto.response.TokenResponse;
import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.repository.UserRepository;
import com.shinhan.esg_be.global.security.JwtTokenProvider; // 1. 추가 확인
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본적으로 읽기 전용으로 설정하여 성능 최적화
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider; // 2. 토큰 발행을 위해 주입 필수!

    /**
     * 회원가입 로직
     */
    @Transactional // 저장 로직이므로 쓰기 권한 활성화
    public void join(AuthJoinRequest req) {
        // 1. 아이디 중복 체크
        if (userRepository.existsByLoginId(req.getLoginId())) {
            throw new RuntimeException("이미 존재하는 아이디입니다.");
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new RuntimeException("이미 사용 중인 이메일입니다.");
        }

        // 3. 전화번호 중복 체크 (추가)
        if (userRepository.existsByPhoneNumber(req.getPhoneNumber().trim())) {
            throw new RuntimeException("이미 등록된 전화번호입니다.");
        }

        // 2. 1인 1계정(CI/DI) 중복 체크
        if (userRepository.existsByCiDi(req.getCiDi())) {
            throw new RuntimeException("이미 가입된 본인인증 정보가 존재합니다.");
        }

        // 3. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(req.getPassword());

        // 4. 유저 엔티티 생성 및 DB 저장
        User user = User.create(
                req.getLoginId(),
                encodedPassword,
                req.getName(),
                req.getEmail(),
                req.getPhoneNumber().trim(),
                req.getBirthdate(),
                req.getCiDi()
        );

        userRepository.save(user);
    }

    /**
     * 로그인 로직 (토큰 반환)
     */
    public TokenResponse login(AuthLoginRequest req) {
        // 1. 아이디 확인
        User user = userRepository.findByLoginId(req.getLoginId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 2. 비밀번호 확인
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }

        // 3. 토큰 생성 및 응답 DTO 반환
        String token = jwtTokenProvider.createToken(user.getLoginId());
        return new TokenResponse(token);
    }
}