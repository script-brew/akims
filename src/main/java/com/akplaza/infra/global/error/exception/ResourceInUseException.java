package com.akplaza.infra.global.error.exception;

// 삭제/수정 시 참조 무결성이 깨질 우려가 있을 때 발생하는 예외
public class ResourceInUseException extends RuntimeException {
    public ResourceInUseException(String message) {
        super(message);
    }
}