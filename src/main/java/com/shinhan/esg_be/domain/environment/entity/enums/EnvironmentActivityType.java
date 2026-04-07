package com.shinhan.esg_be.domain.environment.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum EnvironmentActivityType {
    TUMBLER("tumbler"),
    SHARED_BIKE("shared-bike"),
    EV_RENTAL("ev-rental");

    private final String code;

    EnvironmentActivityType(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static EnvironmentActivityType from(String code) {
        return Arrays.stream(values())
                .filter(value -> value.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown environment activity type: " + code));
    }
}
