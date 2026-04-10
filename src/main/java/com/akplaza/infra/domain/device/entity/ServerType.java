package com.akplaza.infra.domain.device.entity;

// 서버 물리/가상화 타입
public enum ServerType {
    PHYSICAL, // 물리 서버 (Baremetal)
    VIRTUAL, // 가상 서버 (VM, Cloud Instance)
    AWS_CLOUD,
    SCP_CLOUD
}