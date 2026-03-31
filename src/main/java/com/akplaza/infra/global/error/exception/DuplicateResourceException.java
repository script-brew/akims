package com.akplaza.infra.global.error.exception;

// 고유값(이름, IP, Serial 등)이 중복될 때 발생하는 예외
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}