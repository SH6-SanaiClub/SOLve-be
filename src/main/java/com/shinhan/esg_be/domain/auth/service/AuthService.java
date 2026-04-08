package com.shinhan.esg_be.domain.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinhan.esg_be.domain.auth.dto.internal.VerifiedIdentity;
import com.shinhan.esg_be.domain.auth.dto.request.AuthJoinRequest;
import com.shinhan.esg_be.domain.auth.dto.request.AuthLoginRequest;
import com.shinhan.esg_be.domain.auth.dto.response.IdentityVerificationResponse;
import com.shinhan.esg_be.domain.auth.dto.response.TokenResponse;
import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.repository.UserRepository;
import com.shinhan.esg_be.global.exception.BadRequestException;
import com.shinhan.esg_be.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private static final String REFRESH_TOKEN_PREFIX = "RT:";
    private static final String VERIFIED_IDENTITY_PREFIX = "VI:";

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;
    private final PortOneIdentityVerificationService portOneIdentityVerificationService;
    private final ObjectMapper objectMapper;

    @Value("${auth.verification-token-expiration}")
    private long verificationTokenValidityInMilliseconds;

    @Transactional
    public void join(AuthJoinRequest req) {
        VerifiedIdentity verifiedIdentity = getVerifiedIdentity(req.getVerificationToken());
        validateJoinRequest(req, verifiedIdentity.getCiDi());

        User user = User.create(
                req.getLoginId(),
                passwordEncoder.encode(req.getPassword()),
                req.getName(),
                req.getEmail(),
                req.getPhoneNumber().trim(),
                req.getBirthdate(),
                verifiedIdentity.getCiDi()
        );

        userRepository.save(user);
        redisTemplate.delete(getVerifiedIdentityKey(req.getVerificationToken()));
    }

    @Transactional
    public IdentityVerificationResponse verifyIdentity(String impUid) {
        VerifiedIdentity verifiedIdentity = portOneIdentityVerificationService.verify(impUid);
        ensureCiDiNotRegistered(verifiedIdentity.getCiDi());

        String verificationToken = UUID.randomUUID().toString();
        storeVerifiedIdentity(verificationToken, verifiedIdentity);

        return new IdentityVerificationResponse(true, verificationToken);
    }

    @Transactional
    public TokenResponse login(AuthLoginRequest req) {
        User user = userRepository.findByLoginId(req.getLoginId())
                .orElseThrow(() -> new BadRequestException("사용자를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BadRequestException("비밀번호가 일치하지 않습니다.");
        }

        return issueTokenPair(user.getUserId(), user.getLoginId());
    }

    @Transactional
    public TokenResponse reissue(String refreshToken) {
        TokenSubject tokenSubject = validateRefreshToken(refreshToken);
        String savedRefreshToken = redisTemplate.opsForValue().get(getRefreshTokenKey(tokenSubject.loginId()));

        if (savedRefreshToken == null || !savedRefreshToken.equals(refreshToken)) {
            throw new BadRequestException("저장된 리프레시 토큰과 일치하지 않습니다.");
        }

        return issueTokenPair(tokenSubject.userId(), tokenSubject.loginId());
    }

    @Transactional
    public void logout(String refreshToken) {
        TokenSubject tokenSubject = validateRefreshToken(refreshToken);
        redisTemplate.delete(getRefreshTokenKey(tokenSubject.loginId()));
    }

    public boolean checkLoginIdDuplicate(String loginId) {
        return userRepository.existsByLoginId(loginId);
    }

    private void validateJoinRequest(AuthJoinRequest req, String ciDi) {
        if (userRepository.existsByLoginId(req.getLoginId())) {
            throw new BadRequestException("이미 존재하는 아이디입니다.");
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new BadRequestException("이미 사용 중인 이메일입니다.");
        }
        if (userRepository.existsByPhoneNumber(req.getPhoneNumber().trim())) {
            throw new BadRequestException("이미 등록된 전화번호입니다.");
        }
        ensureCiDiNotRegistered(ciDi);
    }

    private void ensureCiDiNotRegistered(String ciDi) {
        if (userRepository.existsByCiDi(ciDi)) {
            throw new BadRequestException("이미 가입된 본인 인증 정보입니다.");
        }
    }

    private TokenSubject validateRefreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken) || !jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new BadRequestException("유효하지 않은 리프레시 토큰입니다.");
        }
        return new TokenSubject(
                jwtTokenProvider.getUserId(refreshToken),
                jwtTokenProvider.getLoginId(refreshToken)
        );
    }

    private TokenResponse issueTokenPair(Long userId, String loginId) {
        String accessToken = jwtTokenProvider.createAccessToken(userId, loginId);
        String refreshToken = jwtTokenProvider.createRefreshToken(userId, loginId);

        redisTemplate.opsForValue().set(
                getRefreshTokenKey(loginId),
                refreshToken,
                jwtTokenProvider.getRefreshTokenValidityInMilliseconds(),
                TimeUnit.MILLISECONDS
        );

        return new TokenResponse(accessToken, refreshToken);
    }

    private void storeVerifiedIdentity(String verificationToken, VerifiedIdentity verifiedIdentity) {
        try {
            redisTemplate.opsForValue().set(
                    getVerifiedIdentityKey(verificationToken),
                    objectMapper.writeValueAsString(verifiedIdentity),
                    verificationTokenValidityInMilliseconds,
                    TimeUnit.MILLISECONDS
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("인증 정보를 저장하지 못했습니다.", e);
        }
    }

    private VerifiedIdentity getVerifiedIdentity(String verificationToken) {
        String value = redisTemplate.opsForValue().get(getVerifiedIdentityKey(verificationToken));

        if (value == null) {
            throw new BadRequestException("본인인증 정보가 없거나 만료되었습니다.");
        }

        try {
            return objectMapper.readValue(value, VerifiedIdentity.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("인증 정보를 읽지 못했습니다.", e);
        }
    }

    private String getRefreshTokenKey(String loginId) {
        return REFRESH_TOKEN_PREFIX + loginId;
    }

    private String getVerifiedIdentityKey(String verificationToken) {
        return VERIFIED_IDENTITY_PREFIX + verificationToken;
    }

    private record TokenSubject(Long userId, String loginId) {
    }
}
