package com.akplaza.infra.global.error.exception;

// 자산을 찾을 수 없을 때 발생하는 예외
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
