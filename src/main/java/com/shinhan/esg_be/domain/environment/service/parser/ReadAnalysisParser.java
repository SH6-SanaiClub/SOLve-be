package com.shinhan.esg_be.domain.environment.service.parser;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ReadAnalysisParser {

    private static final Pattern QR_ID_PATTERN = Pattern.compile("QR\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CAR_NUMBER_PATTERN = Pattern.compile("(\\d{2,3}[가-힣]\\d{4})");
    private static final Pattern PERCENT_PATTERN = Pattern.compile("^\\d+%$");

    private static final Pattern RIDE_TIME_VALUE_PATTERN = Pattern.compile("(\\d+\\s*분)");
    private static final Pattern DISTANCE_VALUE_PATTERN =
            Pattern.compile("(\\d+(?:\\.\\d+)?\\s*(?:km|m))", Pattern.CASE_INSENSITIVE);
    private static final Pattern PAYMENT_DATETIME_VALUE_PATTERN =
            Pattern.compile("(\\d{2,4}[./-]\\d{1,2}[./-]\\d{1,2}\\s+\\d{1,2}:\\d{2})");
    private static final Pattern RENTAL_PERIOD_VALUE_PATTERN =
            Pattern.compile("(\\d{1,2}[./-]\\d{1,2}.*\\d{1,2}:\\d{2}\\s*[~~-]\\s*\\d{1,2}:\\d{2})");
    private static final Pattern AMOUNT_VALUE_PATTERN =
            Pattern.compile("((?:\\d{1,3}(?:,\\d{3})*|\\d+)\\s*원)");

    private static final List<String> COMPLETION_KEYWORDS = List.of(
            "반납완료",
            "반납 완료",
            "주차 반납 인증",
            "반납 인증",
            "이용완료",
            "이용 완료",
            "성공"
    );

    private static final List<String> RIDE_TIME_LABELS = List.of("이용 시간", "이용시간", "주행 시간", "주행시간");
    private static final List<String> DISTANCE_LABELS = List.of("이동 거리", "이동거리", "주행 거리", "주행거리");
    private static final List<String> PAYMENT_DATE_LABELS = List.of("결제 일시", "결제일시", "이용 일시", "이용일시", "결제일");
    private static final List<String> PAYMENT_METHOD_LABELS = List.of("결제 수단", "결제수단");
    private static final List<String> FINAL_AMOUNT_LABELS =
            List.of("최종 결제 금액", "최종결제금액", "총 결제 금액", "총결제금액", "결제 금액", "결제금액");

    private static final List<String> RENTAL_PERIOD_LABELS = List.of("대여 기간", "대여기간", "이용 기간", "이용기간");
    private static final List<String> RENTAL_TYPE_LABELS = List.of("대여 유형", "대여유형", "이용 유형", "이용유형");
    private static final List<String> PICKUP_LOCATION_LABELS =
            List.of("대여 위치", "대여위치", "승차 위치", "승차위치", "출발 위치", "출발위치");
    private static final List<String> RETURN_LOCATION_LABELS =
            List.of("반납 위치", "반납위치", "하차 위치", "하차위치", "도착 위치", "도착위치");
    private static final List<String> PREPAID_AMOUNT_LABELS =
            List.of("운행 전 결제 금액", "운행 전 결제금액", "운행전 결제금액", "운행전결제금액", "사전 결제 금액", "사전결제금액");
    private static final List<String> RENTAL_FEE_LABELS =
            List.of("대여 요금", "대여요금", "이용 요금", "이용요금", "주행 요금", "주행요금");
    private static final List<String> INSURANCE_FEE_LABELS = List.of("보험료", "면책금");

    private static final List<String> FUEL_KEYWORDS = List.of("가솔린", "디젤", "휘발유", "전기", "EV", "BEV");

    private static final List<String> KNOWN_LABELS = List.of(
            "이용 시간", "이용시간", "주행 시간", "주행시간",
            "이동 거리", "이동거리", "주행 거리", "주행거리",
            "결제 일시", "결제일시", "이용 일시", "이용일시", "결제일",
            "결제 수단", "결제수단",
            "최종 결제 금액", "최종결제금액", "총 결제 금액", "총결제금액", "결제 금액", "결제금액",
            "대여 기간", "대여기간", "이용 기간", "이용기간",
            "대여 유형", "대여유형", "이용 유형", "이용유형",
            "대여 위치", "대여위치", "승차 위치", "승차위치", "출발 위치", "출발위치",
            "반납 위치", "반납위치", "하차 위치", "하차위치", "도착 위치", "도착위치",
            "결제정보",
            "운행 전 결제 금액", "운행 전 결제금액", "운행전 결제금액", "운행전결제금액", "사전 결제 금액", "사전결제금액",
            "대여 요금", "대여요금", "이용 요금", "이용요금", "주행 요금", "주행요금",
            "보험료", "면책금"
    );

    public ParsedEnvironmentData parse(JsonNode rawResult) {
        JsonNode analyzeResult = rawResult.path("analyzeResult");
        String rawText = readText(analyzeResult.path("content"));

        List<String> lines = mergeLines(
                extractLines(analyzeResult.path("pages")),
                extractLinesFromRawText(rawText)
        );

        String providerAndQrLine = firstNonNull(
                findLineContaining(lines, "/ QR"),
                findLineContaining(lines, "QR ")
        );

        String completionText = firstNonNull(
                findKeywordInLines(lines, COMPLETION_KEYWORDS),
                findKeywordInText(rawText, COMPLETION_KEYWORDS)
        );

        String vehicleInfoLine = findVehicleInfoLine(lines);
        String fuelInfoLine = firstNonNull(
                findFuelInfoLine(lines),
                findFuelInfoLine(extractLinesFromRawText(rawText))
        );

        String rideTimeText = preferMatchedValue(
                findValueByLabels(lines, RIDE_TIME_LABELS),
                rawText,
                RIDE_TIME_VALUE_PATTERN
        );
        String distanceText = preferMatchedValue(
                findValueByLabels(lines, DISTANCE_LABELS),
                rawText,
                DISTANCE_VALUE_PATTERN
        );
        String paymentDateTimeText = preferMatchedValue(
                findValueByLabels(lines, PAYMENT_DATE_LABELS),
                rawText,
                PAYMENT_DATETIME_VALUE_PATTERN
        );
        String finalAmountText = preferMatchedValue(
                firstNonNull(
                        findValueByLabels(lines, FINAL_AMOUNT_LABELS),
                        extractAmountAroundLabels(lines, FINAL_AMOUNT_LABELS)
                ),
                rawText,
                AMOUNT_VALUE_PATTERN
        );
        String rentalPeriodText = preferMatchedValue(
                findValueByLabels(lines, RENTAL_PERIOD_LABELS),
                rawText,
                RENTAL_PERIOD_VALUE_PATTERN
        );

        return ParsedEnvironmentData.builder()
                .rawText(rawText)
                .lines(lines)
                .providerName(extractProviderName(providerAndQrLine))
                .bikeQrId(extractBikeQrId(providerAndQrLine))
                .completionText(completionText)
                .rideTimeText(rideTimeText)
                .distanceText(distanceText)
                .paymentDateTimeText(paymentDateTimeText)
                .paymentMethod(findValueByLabels(lines, PAYMENT_METHOD_LABELS))
                .finalAmountText(finalAmountText)
                .vehicleModel(extractVehicleModel(vehicleInfoLine))
                .fuelType(extractFuelType(fuelInfoLine))
                .carNumber(extractCarNumber(vehicleInfoLine))
                .rentalPeriodText(rentalPeriodText)
                .rentalType(findValueByLabels(lines, RENTAL_TYPE_LABELS))
                .pickupLocation(normalizeLocation(findValueByLabels(lines, PICKUP_LOCATION_LABELS)))
                .returnLocation(normalizeLocation(findValueByLabels(lines, RETURN_LOCATION_LABELS)))
                .prepaidAmountText(firstNonNull(
                        findValueByLabels(lines, PREPAID_AMOUNT_LABELS),
                        extractAmountAroundLabels(lines, PREPAID_AMOUNT_LABELS)
                ))
                .rentalFeeText(firstNonNull(
                        findValueByLabels(lines, RENTAL_FEE_LABELS),
                        extractAmountAroundLabels(lines, RENTAL_FEE_LABELS)
                ))
                .insuranceFeeText(firstNonNull(
                        findValueByLabels(lines, INSURANCE_FEE_LABELS),
                        extractAmountAroundLabels(lines, INSURANCE_FEE_LABELS)
                ))
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

    private List<String> extractLinesFromRawText(String rawText) {
        List<String> lines = new ArrayList<>();

        if (rawText == null || rawText.isBlank()) {
            return lines;
        }

        for (String line : rawText.split("\\R")) {
            String cleaned = cleanValue(line);
            if (cleaned != null) {
                lines.add(cleaned);
            }
        }

        return lines;
    }

    private List<String> mergeLines(List<String> primaryLines, List<String> fallbackLines) {
        List<String> merged = new ArrayList<>(primaryLines.size() + fallbackLines.size());
        merged.addAll(primaryLines);
        merged.addAll(fallbackLines);
        return merged;
    }

    private String findValueByLabels(List<String> lines, List<String> labels) {
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);

            String inlineValue = findInlineValue(line, labels);
            if (inlineValue != null) {
                return inlineValue;
            }

            if (isLabelLine(line, labels)) {
                String nextValue = findNextValue(lines, i, labels);
                if (nextValue != null) {
                    return nextValue;
                }
            }
        }

        return null;
    }

    private String findInlineValue(String line, List<String> labels) {
        for (String label : labels) {
            Matcher separatorMatcher = Pattern.compile("^\\Q" + label + "\\E\\s*[:：-]\\s*(.+)$").matcher(line);
            if (separatorMatcher.find()) {
                return cleanValue(separatorMatcher.group(1));
            }

            Matcher spacedMatcher = Pattern.compile("^\\Q" + label + "\\E\\s+(.+)$").matcher(line);
            if (spacedMatcher.find()) {
                return cleanValue(spacedMatcher.group(1));
            }
        }

        return null;
    }

    private boolean isLabelLine(String line, List<String> labels) {
        String normalizedLine = normalizeText(stripTrailingSeparator(line));

        for (String label : labels) {
            if (normalizedLine.equals(normalizeText(label))) {
                return true;
            }
        }

        return false;
    }

    private String findNextValue(List<String> lines, int labelIndex, List<String> labels) {
        boolean locationLabel = isLocationLabel(labels);

        for (int i = labelIndex + 1; i < lines.size() && i <= labelIndex + 8; i++) {
            String candidate = cleanValue(lines.get(i));
            if (candidate == null) {
                continue;
            }

            if (isKnownLabel(candidate) || isLabelFragment(candidate)) {
                continue;
            }

            if (locationLabel) {
                return extractLocationValue(lines, i);
            }

            return candidate;
        }

        return null;
    }

    private String extractLocationValue(List<String> lines, int startIndex) {
        List<String> parts = new ArrayList<>();

        for (int i = startIndex; i < lines.size() && i <= startIndex + 3; i++) {
            String candidate = cleanValue(lines.get(i));
            if (candidate == null) {
                continue;
            }

            if (isKnownLabel(candidate)) {
                if (!parts.isEmpty()) {
                    break;
                }
                continue;
            }

            parts.add(candidate);
            if (!candidate.equals("G")) {
                break;
            }
        }

        if (parts.isEmpty()) {
            return null;
        }

        return cleanValue(String.join(" ", parts));
    }

    private boolean isLocationLabel(List<String> labels) {
        return labels == PICKUP_LOCATION_LABELS || labels == RETURN_LOCATION_LABELS;
    }

    private boolean isKnownLabel(String value) {
        String normalizedValue = normalizeText(stripTrailingSeparator(value));

        for (String label : KNOWN_LABELS) {
            if (normalizedValue.equals(normalizeText(label))) {
                return true;
            }
        }

        return false;
    }

    private boolean isLabelFragment(String value) {
        return List.of("일시", "시간", "위치", "금액", "수단", "요금").contains(normalizeText(value));
    }

    private String stripTrailingSeparator(String value) {
        return value == null ? "" : value.replaceAll("[:：-]+$", "").trim();
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }

        return value.replace(" ", "").trim();
    }

    private String findLineContaining(List<String> lines, String keyword) {
        for (String line : lines) {
            if (line.contains(keyword)) {
                return line;
            }
        }
        return null;
    }

    private String findKeywordInLines(List<String> lines, List<String> keywords) {
        for (String line : lines) {
            for (String keyword : keywords) {
                if (line.contains(keyword)) {
                    return keyword;
                }
            }
        }

        return null;
    }

    private String findKeywordInText(String rawText, List<String> keywords) {
        if (rawText == null || rawText.isBlank()) {
            return null;
        }

        for (String keyword : keywords) {
            if (rawText.contains(keyword)) {
                return keyword;
            }
        }

        return null;
    }

    private String findVehicleInfoLine(List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (extractCarNumber(line) == null) {
                continue;
            }

            String model = extractVehicleModel(line);
            if (model != null) {
                return line;
            }

            String previousLine = findPreviousVehicleModelLine(lines, i);
            if (previousLine != null) {
                return previousLine + " " + line;
            }

            return line;
        }

        return null;
    }

    private String findPreviousVehicleModelLine(List<String> lines, int currentIndex) {
        for (int i = currentIndex - 1; i >= 0 && i >= currentIndex - 3; i--) {
            String candidate = cleanValue(lines.get(i));
            if (candidate == null) {
                continue;
            }

            if (isKnownLabel(candidate) || PERCENT_PATTERN.matcher(candidate).matches()) {
                continue;
            }

            if (extractCarNumber(candidate) == null) {
                return candidate;
            }
        }

        return null;
    }

    private String findFuelInfoLine(List<String> lines) {
        for (String line : lines) {
            for (String keyword : FUEL_KEYWORDS) {
                if (line.toLowerCase().contains(keyword.toLowerCase())) {
                    return line;
                }
            }
        }

        return null;
    }

    private String extractProviderName(String providerAndQrLine) {
        if (providerAndQrLine == null) {
            return null;
        }

        String[] parts = providerAndQrLine.split("/");
        if (parts.length == 0) {
            return cleanValue(providerAndQrLine);
        }

        return cleanValue(parts[0]);
    }

    private String extractBikeQrId(String providerAndQrLine) {
        if (providerAndQrLine == null) {
            return null;
        }

        Matcher matcher = QR_ID_PATTERN.matcher(providerAndQrLine);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    private String extractVehicleModel(String vehicleInfoLine) {
        if (vehicleInfoLine == null) {
            return null;
        }

        String withoutCarNumber = CAR_NUMBER_PATTERN.matcher(vehicleInfoLine).replaceAll("").trim();
        return cleanValue(withoutCarNumber);
    }

    private String extractCarNumber(String vehicleInfoLine) {
        if (vehicleInfoLine == null) {
            return null;
        }

        Matcher matcher = CAR_NUMBER_PATTERN.matcher(vehicleInfoLine);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    private String extractFuelType(String fuelInfoLine) {
        if (fuelInfoLine == null) {
            return null;
        }

        String[] parts = fuelInfoLine.split("\\|");
        return cleanValue(parts[0]);
    }

    private String extractAmountAroundLabels(List<String> lines, List<String> labels) {
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);

            if (!containsAnyLabel(line, labels)) {
                continue;
            }

            String sameLineAmount = extractFirstMatch(line, AMOUNT_VALUE_PATTERN);
            if (sameLineAmount != null) {
                return sameLineAmount;
            }

            for (int j = i + 1; j < lines.size() && j <= i + 3; j++) {
                String nextLineAmount = extractFirstMatch(lines.get(j), AMOUNT_VALUE_PATTERN);
                if (nextLineAmount != null) {
                    return nextLineAmount;
                }
            }
        }

        return null;
    }

    private boolean containsAnyLabel(String line, List<String> labels) {
        for (String label : labels) {
            if (normalizeText(line).contains(normalizeText(label))) {
                return true;
            }
        }

        return false;
    }

    private String preferMatchedValue(String candidate, String rawText, Pattern pattern) {
        if (hasMatch(candidate, pattern)) {
            return cleanValue(candidate);
        }

        return extractFirstMatch(rawText, pattern);
    }

    private boolean hasMatch(String value, Pattern pattern) {
        return value != null && pattern.matcher(value).find();
    }

    private String extractFirstMatch(String value, Pattern pattern) {
        if (value == null || value.isBlank()) {
            return null;
        }

        Matcher matcher = pattern.matcher(value);
        if (matcher.find()) {
            return cleanValue(matcher.group(1));
        }

        return null;
    }

    private String normalizeLocation(String location) {
        if (location == null) {
            return null;
        }

        String normalized = location.replace(">", "").trim();
        if (normalized.startsWith("G ")) {
            normalized = normalized.substring(2).trim();
        }
        if (normalized.equals("G")) {
            return null;
        }

        return cleanValue(normalized);
    }

    private String cleanValue(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String readText(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }

        return cleanValue(node.asText(null));
    }

    private String firstNonNull(String... values) {
        for (String value : values) {
            if (value != null) {
                return value;
            }
        }

        return null;
    }
}
