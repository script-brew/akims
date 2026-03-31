// 1-2. NetworkDeviceUpdateRequest.java (수정 요청 DTO)
package com.akplaza.infra.domain.device.dto;

import com.akplaza.infra.domain.device.entity.NetworkDeviceCategory;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NetworkDeviceUpdateRequest {
    private String name;
    private NetworkDeviceCategory category;
    private String os;
    private String description;
    private boolean ha;
    private String monitoringInfo;
    private Long hardwareId;
}