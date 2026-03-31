// 1-1. NetworkDeviceCreateRequest.java (등록 요청 DTO)
package com.akplaza.infra.domain.device.dto;

import java.util.ArrayList;
import java.util.List;

import com.akplaza.infra.domain.device.entity.NetworkDeviceCategory;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NetworkDeviceCreateRequest {
    private String name; // 장비명 (HostName)
    private NetworkDeviceCategory category; // L2, L3, FIREWALL 등
    private String os;
    private String description;
    private boolean ha;
    private String monitoringInfo;

    private Long hardwareId; // 물리 하드웨어 매핑 ID (가상 어플라이언스인 경우 null 허용)

    private List<String> ipAddresses = new ArrayList<>(); // 할당할 IP 목록 (VIP, 관리 IP 등)
}