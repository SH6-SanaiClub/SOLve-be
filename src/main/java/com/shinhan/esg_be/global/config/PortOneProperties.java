package com.shinhan.esg_be.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "portone")
public class PortOneProperties {

    private String apiBaseUrl = "https://api.iamport.kr";
    private String apiVersion = "v1";
    private String apiKey;
    private String apiSecret;
    private String impCode;

    private String paymentNamePrefix = "Sanai_Contract_";
    private String paymentCurrency = "KRW";
    private String paymentLanguage = "ko";

    private String webhookPath = "/payment/webhook";
    private boolean webhookEnabled = true;

    private String refundReasonDefault = "고객 요청에 의한 환불";
    private boolean refundChecksumEnabled = true;

    private int apiConnectTimeout = 60000;
    private int apiReadTimeout = 60000;
    private int apiRetryCount = 3;
    private int apiRetryDelay = 1000;

    private boolean allowMissingCiDiFallback = true;
    private boolean logEnabled = true;
    private String logLevel = "INFO";
}
