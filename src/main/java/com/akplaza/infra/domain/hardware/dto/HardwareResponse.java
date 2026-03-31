// 1-3. HardwareResponse.java (조회 응답 DTO)
package com.akplaza.infra.domain.hardware.dto;

import com.akplaza.infra.domain.hardware.entity.Hardware;

import lombok.Getter;

@Getter
public class HardwareResponse {
    private Long id;
    private String model;
    private String serialNo;
    private String equipmentType;
    private Integer introductionYear;
    private Integer size;
    private Boolean isSinglePower;
    private String description;

    // 위치 정보 (조인 데이터)
    private Long rackId;
    private String rackNo;
    private Integer rackPosition;
    private String locationName; // 장소명 (예: 상암 데이터센터)

    public HardwareResponse(Hardware hardware) {
        this.id = hardware.getId();
        this.model = hardware.getModel();
        this.serialNo = hardware.getSerialNo();
        this.equipmentType = hardware.getEquipmentType().name();
        this.introductionYear = hardware.getIntroductionYear();
        this.size = hardware.getSize();
        this.description = hardware.getDescription();
        this.isSinglePower = hardware.getIsSinglePower();

        // Rack이 할당되어 있는 경우 (Null-safe)
        if (hardware.getRack() != null) {
            this.rackId = hardware.getRack().getId();
            this.rackNo = hardware.getRack().getRackNo();
            this.rackPosition = hardware.getRackPosition();

            if (hardware.getRack().getLocation() != null) {
                this.locationName = hardware.getRack().getLocation().getName();
            }
        }
    }
}