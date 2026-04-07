package com.shinhan.esg_be.domain.environment.service.validator;

public record ValidationResult(boolean approved, String reason) {

    public static ValidationResult success() {
        return new ValidationResult(true, null);
    }

    public static ValidationResult rejected(String reason) {
        return new ValidationResult(false, reason);
    }
}
