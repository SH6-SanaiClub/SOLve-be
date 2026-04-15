package com.shinhan.esg_be.domain.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinhan.esg_be.domain.auth.dto.internal.VerifiedIdentity;
import com.shinhan.esg_be.domain.auth.dto.request.AuthJoinRequest;
import com.shinhan.esg_be.domain.score.service.ScoreService;
import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.repository.UserRepository;
import com.shinhan.esg_be.global.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    @Test
    @DisplayName("신규 회원가입 시 기본 점수 이력을 초기화한다")
    void initializeScoreWhenJoinNewUser() throws Exception {
        UserRepository userRepository = mock(UserRepository.class);
        BCryptPasswordEncoder passwordEncoder = mock(BCryptPasswordEncoder.class);
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        PortOneIdentityVerificationService portOneIdentityVerificationService = mock(PortOneIdentityVerificationService.class);
        ScoreService scoreService = mock(ScoreService.class);
        UserSessionCleanupService userSessionCleanupService = mock(UserSessionCleanupService.class);
        ObjectMapper objectMapper = new ObjectMapper();
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);

        AuthService authService = new AuthService(
                userRepository,
                passwordEncoder,
                jwtTokenProvider,
                redisTemplate,
                portOneIdentityVerificationService,
                objectMapper,
                scoreService,
                userSessionCleanupService
        );

        AuthJoinRequest request = createJoinRequest();
        String verifiedIdentityJson = objectMapper.writeValueAsString(new VerifiedIdentity("ci-di-new-user"));

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("VI:" + request.getVerificationToken())).thenReturn(verifiedIdentityJson);
        when(userRepository.findByCiDi("ci-di-new-user")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "userId", 100L);
            return user;
        });

        authService.join(request);

        verify(userRepository).save(any(User.class));
        verify(scoreService).initializeUserScore(100L);
        verify(redisTemplate).delete("VI:" + request.getVerificationToken());
    }

    private AuthJoinRequest createJoinRequest() {
        AuthJoinRequest request = newInstance(AuthJoinRequest.class);
        request.setLoginId("new-user");
        request.setPassword("password");
        request.setName("신규회원");
        request.setEmail("new-user@test.com");
        request.setPhoneNumber("010-1234-5678");
        request.setBirthdate(LocalDate.of(2000, 1, 1));
        request.setVerificationToken("verification-token");
        return request;
    }

    private <T> T newInstance(Class<T> type) {
        try {
            var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to instantiate " + type.getSimpleName(), exception);
        }
    }
}
