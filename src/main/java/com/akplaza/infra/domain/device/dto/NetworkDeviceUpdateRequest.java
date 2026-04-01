// 1-2. NetworkDeviceUpdateRequest.java (수정 요청 DTO)
package com.akplaza.infra.domain.device.dto;

import com.akplaza.infra.domain.device.entity.NetworkDeviceCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NetworkDeviceUpdateRequest {

    @NotBlank(message = "변경할 장비명(HostName)은 필수 입력값입니다.")
    private String name;

    @NotNull(message = "장비 분류(Category)를 선택해주세요.")
    private NetworkDeviceCategory category;

    @NotBlank(message = "운영체제(OS) 정보는 필수입니다.")
    private String os;

    private String description;
    private boolean ha;
    private String monitoringInfo;
    private Long hardwareId;

    private Long ipCidrId;
    private String ipAddress;

}