package com.shinhan.esg_be.domain.social.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinhan.esg_be.global.config.PortOneProperties;
import com.shinhan.esg_be.global.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class PortOnePaymentService {

    private static final int SUCCESS_CODE = 0;
    private static final String PAID_STATUS = "paid";

    private final ObjectMapper objectMapper;
    private final PortOneProperties portOneProperties;

    public VerifiedPayment verify(String impUid) {
        validateCredentials();

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(portOneProperties.getApiConnectTimeout()))
                .build();

        String accessToken = getAccessToken(httpClient);

        try {
            HttpResponse<String> response = httpClient.send(
                    buildPaymentLookupRequest(accessToken, "/payments/" + impUid),
                    HttpResponse.BodyHandlers.ofString()
            );
            validateSuccessStatus(response.statusCode(), "PortOne 결제 조회에 실패했습니다.");
            return extractVerifiedPayment(response.body(), impUid);
        } catch (IOException e) {
            throw new RuntimeException("PortOne 결제 응답 파싱에 실패했습니다.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("PortOne 결제 조회 중 인터럽트가 발생했습니다.", e);
        }
    }

    private HttpRequest buildPaymentLookupRequest(String accessToken, String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create(portOneProperties.getApiBaseUrl() + path + "?include_sandbox=true"))
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .timeout(Duration.ofMillis(portOneProperties.getApiReadTimeout()))
                .GET()
                .build();
    }

    private VerifiedPayment extractVerifiedPayment(String responseBody, String impUid) throws IOException {
        PaymentApiResponse apiResponse = objectMapper.readValue(responseBody, PaymentApiResponse.class);
        if (apiResponse == null || apiResponse.code != SUCCESS_CODE || apiResponse.response == null) {
            throw new BadRequestException("PortOne 결제 응답이 올바르지 않습니다.");
        }

        PaymentData payment = apiResponse.response;
        if (!PAID_STATUS.equalsIgnoreCase(payment.status)) {
            throw new BadRequestException("결제가 완료되지 않았습니다.");
        }

        return new VerifiedPayment(
                payment.impUid,
                payment.merchantUid,
                payment.amount == null ? 0L : payment.amount.longValue(),
                defaultText(payment.payMethod),
                defaultText(payment.status),
                toLocalDateTime(payment.paidAt),
                payment.failReason,
                defaultText(payment.buyerName),
                defaultText(payment.buyerEmail),
                defaultText(payment.buyerTel),
                defaultText(payment.pgProvider),
                defaultText(payment.pgTid),
                defaultText(payment.cardName),
                defaultText(payment.cardNumber),
                defaultText(payment.receiptUrl)
        );
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
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            validateSuccessStatus(response.statusCode(), "PortOne 액세스 토큰 발급에 실패했습니다.");

            TokenResponse tokenResponse = objectMapper.readValue(response.body(), TokenResponse.class);
            if (tokenResponse == null || tokenResponse.code != SUCCESS_CODE || tokenResponse.response == null) {
                throw new RuntimeException("PortOne 액세스 토큰 응답이 올바르지 않습니다.");
            }
            if (tokenResponse.response.accessToken == null || tokenResponse.response.accessToken.isBlank()) {
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
        if (portOneProperties.getApiKey() == null || portOneProperties.getApiKey().isBlank()) {
            throw new RuntimeException("PortOne API key가 설정되지 않았습니다.");
        }
        if (portOneProperties.getApiSecret() == null || portOneProperties.getApiSecret().isBlank()) {
            throw new RuntimeException("PortOne API secret이 설정되지 않았습니다.");
        }
    }

    private void validateSuccessStatus(int statusCode, String message) {
        if (statusCode < 200 || statusCode >= 300) {
            throw new BadRequestException(message + " status=" + statusCode);
        }
    }

    private LocalDateTime toLocalDateTime(Long epochSeconds) {
        if (epochSeconds == null) {
            return LocalDateTime.now();
        }
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneId.systemDefault());
    }

    private String defaultText(String value) {
        return value == null ? "" : value;
    }

    public record VerifiedPayment(
            String impUid,
            String merchantUid,
            Long amount,
            String paymentMethod,
            String paymentStatus,
            LocalDateTime paidAt,
            String failedReason,
            String buyerName,
            String buyerEmail,
            String buyerTel,
            String pgProvider,
            String pgTid,
            String cardName,
            String cardNumber,
            String receiptUrl
    ) {
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
    private static class PaymentApiResponse {
        public int code;
        public PaymentData response;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class PaymentData {
        @JsonProperty("imp_uid")
        public String impUid;
        @JsonProperty("merchant_uid")
        public String merchantUid;
        public BigDecimal amount;
        public String status;
        @JsonProperty("pay_method")
        public String payMethod;
        @JsonProperty("paid_at")
        public Long paidAt;
        @JsonProperty("fail_reason")
        public String failReason;
        @JsonProperty("buyer_name")
        public String buyerName;
        @JsonProperty("buyer_email")
        public String buyerEmail;
        @JsonProperty("buyer_tel")
        public String buyerTel;
        @JsonProperty("pg_provider")
        public String pgProvider;
        @JsonProperty("pg_tid")
        public String pgTid;
        @JsonProperty("card_name")
        public String cardName;
        @JsonProperty("card_number")
        public String cardNumber;
        @JsonProperty("receipt_url")
        public String receiptUrl;
    }
}
