package com.shinhan.esg_be.domain.environment.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Getter
@Component
public class AzureDocumentIntelligenceProperties {

    @Value("${AZURE_DOCINT_ENDPOINT:}")
    private String endpoint;

    @Value("${AZURE_DOCINT_KEY:}")
    private String key;

    @Value("${AZURE_DOCINT_API_VERSION:2024-11-30}")
    private String apiVersion;

    public boolean isConfigured() {
        return StringUtils.hasText(endpoint) && StringUtils.hasText(key);
    }
}
