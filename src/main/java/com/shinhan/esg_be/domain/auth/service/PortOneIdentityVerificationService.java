package com.shinhan.esg_be.domain.auth.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinhan.esg_be.domain.auth.dto.internal.VerifiedIdentity;
import com.shinhan.esg_be.domain.auth.dto.internal.VerifiedIdentityDetails;
import com.shinhan.esg_be.global.config.PortOneProperties;
import com.shinhan.esg_be.global.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortOneIdentityVerificationService {

    private static final int SUCCESS_CODE = 0;

    private final ObjectMapper objectMapper;
    private final PortOneProperties portOneProperties;

    public VerifiedIdentity verify(String impUid) {
        VerifiedIdentityDetails verifiedIdentityDetails = verifyDetails(impUid);
        return new VerifiedIdentity(verifiedIdentityDetails.getCiDi());
    }

    // 번호 변경에 필요한 본인인증 상세 정보를 조회
    public VerifiedIdentityDetails verifyDetails(String impUid) {
        if (!hasText(impUid)) {
            throw new BadRequestException("impUid가 비어 있습니다.");
        }

        validateCredentials();

        HttpClient httpClient = createHttpClient();
        String accessToken = getAccessToken(httpClient);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(portOneProperties.getApiBaseUrl() + "/certifications/" + impUid))
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .timeout(Duration.ofMillis(portOneProperties.getApiReadTimeout()))
                .GET()
                .build();

        try {
            HttpResponse<String> response = sendWithRetry(httpClient, request);
            validateSuccessStatus(response.statusCode(), "PortOne 본인인증 조회에 실패했습니다.");

            CertificationResponse apiResponse =
                    objectMapper.readValue(response.body(), CertificationResponse.class);

            if (apiResponse == null || apiResponse.code != SUCCESS_CODE || apiResponse.response == null) {
                throw new BadRequestException("PortOne 본인인증 응답이 올바르지 않습니다.");
            }

            CertificationData data = apiResponse.response;
            if (!Boolean.TRUE.equals(data.certified)) {
                throw new BadRequestException("본인인증이 완료되지 않았습니다.");
            }

            log.info("PortOne verification succeeded impUid={} derivedCiDi=true", impUid);

            return buildVerifiedIdentityDetails(data);
        } catch (IOException e) {
            throw new RuntimeException("PortOne 본인인증 응답 파싱에 실패했습니다.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("PortOne 본인인증 조회 중 인터럽트가 발생했습니다.", e);
        }
    }

    private String getAccessToken(HttpClient httpClient) {
        String body = String.format(
                "{\"imp_key\":\"%s\",\"imp_secret\":\"%s\"}",
                portOneProperties.getApiKey(),
                portOneProperties.getApiSecret()
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(portOneProperties.getApiBaseUrl() + "/users/getToken"))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .timeout(Duration.ofMillis(portOneProperties.getApiReadTimeout()))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            HttpResponse<String> response = sendWithRetry(httpClient, request);
            validateSuccessStatus(response.statusCode(), "PortOne 액세스 토큰 발급에 실패했습니다.");

            TokenResponse tokenResponse = objectMapper.readValue(response.body(), TokenResponse.class);
            if (tokenResponse == null || tokenResponse.code != SUCCESS_CODE || tokenResponse.response == null) {
                throw new RuntimeException("PortOne 액세스 토큰 응답이 올바르지 않습니다.");
            }

            if (!hasText(tokenResponse.response.accessToken)) {
                throw new RuntimeException("PortOne 액세스 토큰이 비어 있습니다.");
            }

            return tokenResponse.response.accessToken;
        } catch (IOException e) {
            throw new RuntimeException("PortOne 액세스 토큰 응답 파싱에 실패했습니다.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("PortOne 액세스 토큰 발급 중 인터럽트가 발생했습니다.", e);
        }
    }

    private void validateCredentials() {
        if (!hasText(portOneProperties.getApiKey())) {
            throw new RuntimeException("PortOne API key가 설정되지 않았습니다.");
        }
        if (!hasText(portOneProperties.getApiSecret())) {
            throw new RuntimeException("PortOne API secret이 설정되지 않았습니다.");
        }
    }

    private HttpClient createHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(portOneProperties.getApiConnectTimeout()))
                .build();
    }

    private HttpResponse<String> sendWithRetry(HttpClient httpClient, HttpRequest request)
            throws IOException, InterruptedException {
        int attempts = Math.max(1, portOneProperties.getApiRetryCount());
        int delay = Math.max(0, portOneProperties.getApiRetryDelay());

        IOException lastIOException = null;
        InterruptedException lastInterruptedException = null;

        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException e) {
                lastIOException = e;
            } catch (InterruptedException e) {
                lastInterruptedException = e;
                Thread.currentThread().interrupt();
                break;
            }

            if (attempt < attempts && delay > 0) {
                Thread.sleep(delay);
            }
        }

        if (lastInterruptedException != null) {
            throw lastInterruptedException;
        }

        throw lastIOException != null ? lastIOException : new IOException("PortOne API 호출에 실패했습니다.");
    }

    private void validateSuccessStatus(int statusCode, String message) {
        if (statusCode < 200 || statusCode >= 300) {
            throw new BadRequestException(message + " status=" + statusCode);
        }
    }

    // 인증된 이름, 번호, 생년월일로 새 ciDi 값 생성
    private VerifiedIdentityDetails buildVerifiedIdentityDetails(CertificationData data) {
        String name = requireValue(data.name, "PortOne 이름 정보가 없습니다.");
        String phoneNumber = normalizePhoneNumber(requireValue(data.phone, "PortOne 전화번호 정보가 없습니다."));
        String birthdate = requireValue(data.birthday, "PortOne 생년월일 정보가 없습니다.");

        return new VerifiedIdentityDetails(
                createStableCiDi(data),
                name,
                phoneNumber,
                birthdate
        );
    }

    private String createStableCiDi(CertificationData data) {
        String name = requireValue(data.name, "PortOne 이름 정보가 없습니다.");
        String phone = normalizePhoneNumber(requireValue(data.phone, "PortOne 전화번호 정보가 없습니다."));
        String birthday = requireValue(data.birthday, "PortOne 생년월일 정보가 없습니다.");
        String raw = name + "|" + phone + "|" + birthday;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return toHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("CI/DI 대체값 생성에 실패했습니다.", e);
        }
    }

    private String requireValue(String value, String message) {
        if (!hasText(value)) {
            throw new BadRequestException(message);
        }
        return value;
    }

    private String normalizePhoneNumber(String phoneNumber) {
        return phoneNumber.replaceAll("[^0-9]", "");
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TokenResponse {
        public int code;
        public TokenData response;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TokenData {
        @JsonProperty("access_token")
        public String accessToken;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class CertificationResponse {
        public int code;
        public CertificationData response;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class CertificationData {
        public Boolean certified;
        public String name;
        public String phone;
        public String birthday;
    }
}
