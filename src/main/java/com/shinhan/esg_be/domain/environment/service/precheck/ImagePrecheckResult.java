package com.shinhan.esg_be.domain.environment.service.precheck;

public record ImagePrecheckResult(
        boolean allowed,
        String reason
) {

    private static final String ALLOW_FALLBACK_REASON = "뚜렷한 조작 흔적이 확인되지 않았습니다.";
    private static final String REJECT_FALLBACK_REASON = "이미지 조작 또는 편집이 의심됩니다.";

    public static ImagePrecheckResult allow(String reason) {
        return new ImagePrecheckResult(true, normalizeReason(reason, ALLOW_FALLBACK_REASON));
    }

    public static ImagePrecheckResult reject(String reason) {
        return new ImagePrecheckResult(false, normalizeReason(reason, REJECT_FALLBACK_REASON));
    }

    private static String normalizeReason(String reason, String fallback) {
        if (reason == null || reason.isBlank()) {
            return fallback;
        }

        String trimmed = reason.trim();
        if (!containsHangul(trimmed)) {
            return fallback;
        }
        return trimmed;
    }

    private static boolean containsHangul(String value) {
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character >= '\uAC00' && character <= '\uD7A3') {
                return true;
            }
        }
        return false;
    }
}
