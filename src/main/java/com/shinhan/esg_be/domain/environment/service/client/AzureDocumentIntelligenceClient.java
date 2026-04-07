package com.shinhan.esg_be.domain.environment.service.client;

import com.shinhan.esg_be.domain.environment.config.AzureDocumentIntelligenceProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

import java.net.URI;
import java.util.Base64;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Component
@RequiredArgsConstructor
public class AzureDocumentIntelligenceClient {

    private final RestClient azureDocumentIntelligenceRestClient;
    private final AzureDocumentIntelligenceProperties properties;

    public JsonNode analyze(byte[] imageBytes, String modelId) {
        URI operationLocation = startAnalyze(imageBytes, modelId);
        return pollAnalyzeResult(operationLocation);
    }

    private URI startAnalyze(byte[] imageBytes, String modelId) {
        String base64Source = Base64.getEncoder().encodeToString(imageBytes);

        ResponseEntity<Void> response = azureDocumentIntelligenceRestClient.post()
                .uri(
                        "/documentintelligence/documentModels/{modelId}:analyze?_overload=analyzeDocument&api-version={apiVersion}",
                        modelId,
                        properties.getApiVersion()
                )
                .contentType(APPLICATION_JSON)
                .body(Map.of("base64Source", base64Source))
                .retrieve()
                .toBodilessEntity();

        String operationLocation = response.getHeaders().getFirst("Operation-Location");
        if (operationLocation == null || operationLocation.isBlank()) {
            throw new ResponseStatusException(BAD_GATEWAY, "Azure 분석 응답에서 Operation-Location 헤더를 찾을 수 없습니다.");
        }

        return URI.create(operationLocation);
    }

    private JsonNode pollAnalyzeResult(URI operationLocation) {
        for (int attempt = 0; attempt < 10; attempt++) {
            JsonNode result = azureDocumentIntelligenceRestClient.get()
                    .uri(operationLocation)
                    .retrieve()
                    .body(JsonNode.class);

            if (result == null) {
                throw new ResponseStatusException(BAD_GATEWAY, "Azure 분석 결과가 비어 있습니다.");
            }

            String status = result.path("status").asText();

            if ("succeeded".equalsIgnoreCase(status)) {
                return result;
            }

            if ("failed".equalsIgnoreCase(status)) {
                throw new ResponseStatusException(BAD_GATEWAY, "Azure 분석 요청이 실패했습니다.");
            }

            sleepOneSecond();
        }

        throw new ResponseStatusException(BAD_GATEWAY, "Azure 분석 결과 조회 시간이 초과되었습니다.");
    }

    private void sleepOneSecond() {
        try {
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(BAD_GATEWAY, "Azure 분석 결과 조회가 중단되었습니다.");
        }
    }
}
