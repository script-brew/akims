package com.akplaza.infra.domain.device.entity;

// 서버 환경
public enum Environment {
    PRD, // 운영
    STG, // 스테이징/QA
    DEV, // 개발
    DR // 재해복구
}