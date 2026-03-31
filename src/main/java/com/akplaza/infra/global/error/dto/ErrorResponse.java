package com.akplaza.infra.global.error.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
public class ErrorResponse {
    private final LocalDateTime timestamp = LocalDateTime.now();
    private final int status; // HTTP 상태 코드 (예: 400, 404, 500)
    private final String error; // 에러 종류 (예: Bad Request)
    private final String message; // 상세 메시지
    private final List<FieldError> fieldErrors; // 유효성 검사 실패 시 필드별 에러 내용

    @Builder
    public ErrorResponse(int status, String error, String message, List<FieldError> fieldErrors) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.fieldErrors = fieldErrors != null ? fieldErrors : new ArrayList<>();
    }

    @Getter
    @Builder
    public static class FieldError {
        private String field;
        private String value;
        private String reason;
    }
}