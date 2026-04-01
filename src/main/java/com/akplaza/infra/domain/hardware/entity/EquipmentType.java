package com.akplaza.infra.domain.hardware.entity;

public enum EquipmentType {
    SERVER, // 물리 서버// 스위치, , 방화벽 등
    SWITCH, // 네트워크 스위치(L2,L3,L4)
    ROUTER, // 라우터
    SAN, // SAN
    STORAGE, // 물리스토리지
    FIREWALL, // 방화벽
    ETC // 기타
}