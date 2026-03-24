package com.akplaza.infra.domain.network.entity;

public enum AssignedType {
    SERVER, // 물리/가상 서버
    NETWORK_DEVICE, // 스위치, 라우터, 방화벽 등 네트워크 장비
    VIP, // L4, HAProxy 등에서 사용하는 가상 IP
    NONE // 미할당
}