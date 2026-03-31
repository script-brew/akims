// 1-2. HardwareUpdateRequest.java (수정 요청 DTO)
package com.akplaza.infra.domain.hardware.dto;

import com.akplaza.infra.domain.hardware.entity.EquipmentType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class HardwareUpdateRequest {

    @NotBlank(message = "모델명을 입력해주세요.")
    private String model;

    @NotBlank(message = "시리얼 넘버(Serial Number)는 필수입니다.")
    private String serialNo;

    @NotNull(message = "장비 구분을 선택해주세요.")
    private EquipmentType equipmentType;

    @NotNull(message = "도입년도를 입력해주세요.")
    @Min(value = 1990, message = "올바른 도입년도를 입력해주세요.")
    private Integer introductionYear;

    @NotNull(message = "장비의 크기(U)를 입력해주세요.")
    @Min(value = 1, message = "크기는 1U 이상이어야 합니다.")
    private Integer size;

    private String description;
    private Long rackId;
    private Integer rackPosition;
}