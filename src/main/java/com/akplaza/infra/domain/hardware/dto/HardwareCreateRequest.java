// 1-1. HardwareCreateRequest.java (등록 요청 DTO)
package com.akplaza.infra.domain.hardware.dto;

import com.akplaza.infra.domain.hardware.entity.EquipmentType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class HardwareCreateRequest {

    @NotBlank(message = "모델명을 입력해주세요.")
    private String model; // 모델명 (예: PowerEdge R740)

    @NotBlank(message = "시리얼 넘버(Serial Number)는 필수 입력값입니다.")
    private String serialNo; // 🌟 고유 시리얼 넘버 (자산 식별자)

    @NotNull(message = "장비 구분(EquipmentType)을 선택해주세요.")
    private EquipmentType equipmentType; // 장비 구분 (SERVER, SWITCH, STORAGE 등)

    @NotNull(message = "도입년도를 입력해주세요.")
    @Min(value = 1990, message = "올바른 도입년도를 입력해주세요.")
    private Integer introductionYear; // 도입년도 (감가상각 계산용)

    @NotNull(message = "장비의 크기(U)를 입력해주세요.")
    @Min(value = 1, message = "크기는 1U 이상이어야 합니다.")
    private Integer size; // 차지하는 Rack 단위(U) (예: 1U, 2U)

    private Boolean isSinglePower;

    private String description;

    // 위치 매핑 정보
    private Long rackId; // 실장될 Rack의 ID
    private Integer rackPosition; // Rack 내 시작 위치 (예: 10U 위치에 실장)
}