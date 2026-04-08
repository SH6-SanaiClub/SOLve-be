package com.shinhan.esg_be.domain.environment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;

import com.shinhan.esg_be.domain.environment.service.client.AzureDocumentIntelligenceClient;
import tools.jackson.databind.JsonNode;

@Configuration
public class AzureDocumentIntelligenceConfig {

    private final AzureDocumentIntelligenceProperties properties;

    public AzureDocumentIntelligenceConfig(AzureDocumentIntelligenceProperties properties) {
        this.properties = properties;
    }

    @Bean
    public AzureDocumentIntelligenceClient azureDocumentIntelligenceClient() {
        if (!properties.isConfigured()) {
            return new AzureDocumentIntelligenceClient(RestClient.create(), properties) {
                @Override
                public JsonNode analyze(byte[] imageBytes, String modelId) {
                    throw new ResponseStatusException(BAD_GATEWAY, "Azure 문서 인텔리전스 설정이 없습니다.");
                }
            };
        }

        RestClient restClient = RestClient.builder()
                .baseUrl(properties.getEndpoint())
                .defaultHeader("Ocp-Apim-Subscription-Key", properties.getKey())
                .build();

        return new AzureDocumentIntelligenceClient(restClient, properties);
    }
}
