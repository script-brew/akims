// 1-2. HardwareUpdateRequest.java (수정 요청 DTO)
package com.akplaza.infra.domain.hardware.dto;

import com.akplaza.infra.domain.hardware.entity.EquipmentType;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class HardwareUpdateRequest {
    private String model;
    private String serialNo;
    private EquipmentType equipmentType;
    private Integer introductionYear;
    private Integer size;
    private String description;

    // 위치 이동 처리용
    private Long rackId;
    private Integer rackPosition;
}