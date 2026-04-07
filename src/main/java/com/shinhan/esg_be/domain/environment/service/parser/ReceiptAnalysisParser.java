package com.shinhan.esg_be.domain.environment.service.parser;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

// 텀블러 영수증용 파서

@Component
public class ReceiptAnalysisParser {

    public ParsedEnvironmentData parse(JsonNode rawResult) {
        JsonNode analyzeResult = rawResult.path("analyzeResult");
        String rawText = readText(analyzeResult.path("content"));

        List<String> lines = extractLines(analyzeResult.path("pages"));

        JsonNode fields = analyzeResult.path("documents")
                .path(0)
                .path("fields");

        String merchantName = readText(fields.path("MerchantName").path("content"));
        String transactionDateText = readText(fields.path("TransactionDate").path("content"));
        String transactionTimeText = readText(fields.path("TransactionTime").path("content"));
        String totalAmountText = readText(fields.path("Total").path("content"));

        return ParsedEnvironmentData.builder()
                .rawText(rawText)
                .lines(lines)
                .merchantName(merchantName)
                .transactionDateText(transactionDateText)
                .transactionTimeText(transactionTimeText)
                .totalAmountText(totalAmountText)
                .build();
    }

    private List<String> extractLines(JsonNode pages) {
        List<String> lines = new ArrayList<>();

        if (!pages.isArray()) {
            return lines;
        }

        for (JsonNode page : pages) {
            JsonNode pageLines = page.path("lines");

            if (!pageLines.isArray()) {
                continue;
            }

            for (JsonNode line : pageLines) {
                String content = readText(line.path("content"));

                if (content != null) {
                    lines.add(content);
                }
            }
        }

        return lines;
    }

    private String readText(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }

        String value = node.asText(null);
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
