package com.shinhan.esg_be.domain.environment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AzureDocumentIntelligenceConfig {

    private final AzureDocumentIntelligenceProperties properties;

    public AzureDocumentIntelligenceConfig(AzureDocumentIntelligenceProperties properties) {
        this.properties = properties;
    }

    @Bean
    public RestClient azureDocumentIntelligenceRestClient() {
        return RestClient.builder()
                .baseUrl(properties.getEndpoint())
                .defaultHeader("Ocp-Apim-Subscription-Key", properties.getKey())
                .build();
    }
}