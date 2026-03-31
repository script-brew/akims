// 1-1. HardwareCreateRequest.java (등록 요청 DTO)
package com.akplaza.infra.domain.hardware.dto;

import com.akplaza.infra.domain.hardware.entity.EquipmentType;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class HardwareCreateRequest {
    private String model; // 모델명 (예: PowerEdge R740)
    private String serialNo; // 🌟 고유 시리얼 넘버 (자산 식별자)
    private EquipmentType equipmentType; // 장비 구분 (SERVER, SWITCH, STORAGE 등)
    private Integer introductionYear; // 도입년도 (감가상각 계산용)
    private Integer size; // 차지하는 Rack 단위(U) (예: 1U, 2U)
    private String description;

    // 위치 매핑 정보
    private Long rackId; // 실장될 Rack의 ID
    private Integer rackPosition; // Rack 내 시작 위치 (예: 10U 위치에 실장)
}