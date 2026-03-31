package com.akplaza.infra.global.error;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.akplaza.infra.global.error.dto.ErrorResponse;
import com.akplaza.infra.global.error.exception.DuplicateResourceException;
import com.akplaza.infra.global.error.exception.ResourceInUseException;
import com.akplaza.infra.global.error.exception.ResourceNotFoundException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice // 모든 @RestController에서 발생하는 예외를 감지
public class GlobalExceptionHandler {

        // 1. 자산을 찾을 수 없을 때 (404 Not Found)
        @ExceptionHandler(ResourceNotFoundException.class)
        protected ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException e) {
                log.warn("ResourceNotFoundException: {}", e.getMessage());
                ErrorResponse response = ErrorResponse.builder()
                                .status(HttpStatus.NOT_FOUND.value())
                                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                                .message(e.getMessage())
                                .build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        // 2. 자산 중복 또는 사용 중 등 비즈니스 규칙 위반 시 (409 Conflict)
        @ExceptionHandler({ DuplicateResourceException.class, ResourceInUseException.class })
        protected ResponseEntity<ErrorResponse> handleConflictException(RuntimeException e) {
                log.warn("ConflictException: {}", e.getMessage());
                ErrorResponse response = ErrorResponse.builder()
                                .status(HttpStatus.CONFLICT.value())
                                .error(HttpStatus.CONFLICT.getReasonPhrase())
                                .message(e.getMessage())
                                .build();
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        // 3. 사용자의 입력값(DTO) 유효성 검증 실패 시 (400 Bad Request)
        @ExceptionHandler(MethodArgumentNotValidException.class)
        protected ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
                        MethodArgumentNotValidException e) {
                log.warn("MethodArgumentNotValidException: 입력값 검증 실패");

                List<ErrorResponse.FieldError> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                                .map(error -> ErrorResponse.FieldError.builder()
                                                .field(error.getField())
                                                .value(error.getRejectedValue() == null ? ""
                                                                : error.getRejectedValue().toString())
                                                .reason(error.getDefaultMessage())
                                                .build())
                                .collect(Collectors.toList());

                ErrorResponse response = ErrorResponse.builder()
                                .status(HttpStatus.BAD_REQUEST.value())
                                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                                .message("입력값이 올바르지 않습니다. 필드 에러를 확인해주세요.")
                                .fieldErrors(fieldErrors)
                                .build();

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // 4. 그 외 시스템에서 발생하는 모든 예기치 않은 에러 (500 Internal Server Error)
        @ExceptionHandler(Exception.class)
        protected ResponseEntity<ErrorResponse> handleAllExceptions(Exception e) {
                log.error("Unhandled Exception: ", e); // 500 에러는 스택 트레이스를 로그에 남김
                ErrorResponse response = ErrorResponse.builder()
                                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                                .message("서버 내부 오류가 발생했습니다. 관리자에게 문의하세요.")
                                .build();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
}