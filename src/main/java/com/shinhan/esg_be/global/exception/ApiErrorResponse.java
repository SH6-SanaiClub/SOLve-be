package com.shinhan.esg_be.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ApiErrorResponse {

    private final String code;
    private final String message;
    private final LocalDateTime timestamp;
}
