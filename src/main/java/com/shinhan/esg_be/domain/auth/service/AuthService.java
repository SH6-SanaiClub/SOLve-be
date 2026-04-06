package com.shinhan.esg_be.domain.auth.service;

import com.shinhan.esg_be.domain.auth.dto.request.AuthJoinRequest;
import com.shinhan.esg_be.domain.auth.dto.request.AuthLoginRequest;
import com.shinhan.esg_be.domain.auth.dto.response.TokenResponse;
import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.repository.UserRepository;
import com.shinhan.esg_be.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private static final String REFRESH_TOKEN_PREFIX = "RT:";

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;

    @Transactional
    public void join(AuthJoinRequest req) {
        if (userRepository.existsByLoginId(req.getLoginId())) {
            throw new RuntimeException("이미 존재하는 아이디입니다.");
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new RuntimeException("이미 사용 중인 이메일입니다.");
        }
        if (userRepository.existsByPhoneNumber(req.getPhoneNumber().trim())) {
            throw new RuntimeException("이미 등록된 전화번호입니다.");
        }
        if (userRepository.existsByCiDi(req.getCiDi())) {
            throw new RuntimeException("이미 가입한 본인인증 정보가 존재합니다.");
        }

        String encodedPassword = passwordEncoder.encode(req.getPassword());

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

    @Transactional
    public TokenResponse login(AuthLoginRequest req) {
        User user = userRepository.findByLoginId(req.getLoginId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }

        return issueTokenPair(user.getLoginId());
    }

    @Transactional
    public TokenResponse reissue(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken) || !jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new RuntimeException("유효하지 않은 리프레시 토큰입니다.");
        }

        String loginId = jwtTokenProvider.getLoginId(refreshToken);
        String savedRefreshToken = redisTemplate.opsForValue().get(getRefreshTokenKey(loginId));

        if (savedRefreshToken == null || !savedRefreshToken.equals(refreshToken)) {
            throw new RuntimeException("저장된 리프레시 토큰과 일치하지 않습니다.");
        }

        return issueTokenPair(loginId);
    }

    @Transactional
    public void logout(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken) || !jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new RuntimeException("유효하지 않은 리프레시 토큰입니다.");
        }

        String loginId = jwtTokenProvider.getLoginId(refreshToken);
        redisTemplate.delete(getRefreshTokenKey(loginId));
    }

    private TokenResponse issueTokenPair(String loginId) {
        String accessToken = jwtTokenProvider.createAccessToken(loginId);
        String refreshToken = jwtTokenProvider.createRefreshToken(loginId);

        redisTemplate.opsForValue().set(
                getRefreshTokenKey(loginId),
                refreshToken,
                jwtTokenProvider.getRefreshTokenValidityInMilliseconds(),
                TimeUnit.MILLISECONDS
        );

        return new TokenResponse(accessToken, refreshToken);
    }
    public boolean checkLoginIdDuplicate(String loginId) {
        return userRepository.existsByLoginId(loginId);
    }
    private String getRefreshTokenKey(String loginId) {
        return REFRESH_TOKEN_PREFIX + loginId;
    }
}
