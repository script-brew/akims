package com.akplaza.infra.domain.hardware.entity;

public enum EquipmentType {
    SERVER,     // 물리 서버
    NETWORK,    // 스위치, 라우터, 방화벽 등
    STORAGE,    // SAN, NAS 스토리지
    ETC         // 기타 (KVM, PDU 등)
}