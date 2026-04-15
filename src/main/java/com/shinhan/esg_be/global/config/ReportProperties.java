package com.shinhan.esg_be.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.report")
public class ReportProperties {

    private String verificationBaseUrl;
    private String watermarkImageUrl;
    private String verificationPathPrefix = "/report/verify/";
    private Pdf pdf = new Pdf();

    public String buildVerificationPath(String token) {
        return normalizePathPrefix(verificationPathPrefix) + token;
    }

    public String buildVerificationUrl(String token) {
        return normalizeBaseUrl(verificationBaseUrl) + buildVerificationPath(token);
    }

    @Getter
    @Setter
    public static class Pdf {

        private String templatePath;
        private String fontFamily;
        private List<String> fontLocations = new ArrayList<>();
    }

    private String normalizeBaseUrl(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.endsWith("/")) {
            return normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String normalizePathPrefix(String value) {
        String normalized = value == null || value.isBlank() ? "/report/verify/" : value.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (!normalized.endsWith("/")) {
            normalized = normalized + "/";
        }
        return normalized;
    }
}
