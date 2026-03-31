// 1-3. NetworkDeviceResponse.java (조회 응답 DTO)
package com.akplaza.infra.domain.device.dto;

import java.util.List;

import com.akplaza.infra.domain.device.entity.NetworkDevice;

import lombok.Getter;

@Getter
public class NetworkDeviceResponse {
    private Long id;
    private String name;
    private String category;
    private String os;
    private String description;
    private boolean ha;
    private String monitoringInfo;

    // 조인된 하드웨어 및 위치 정보
    private Long hardwareId;
    private String hardwareModel;
    private String locationName;
    private String rackNo;

    // 할당된 IP 목록
    private List<String> ipAddresses;

    public NetworkDeviceResponse(NetworkDevice device, List<String> ipAddresses) {
        this.id = device.getId();
        this.name = device.getName();
        this.category = device.getCategory().name();
        this.os = device.getOs();
        this.description = device.getDescription();
        this.ha = device.isHa();
        this.monitoringInfo = device.getMonitoringInfo();

        // 하드웨어 정보가 매핑되어 있는 경우 (Null-safe)
        if (device.getHardware() != null) {
            this.hardwareId = device.getHardware().getId();
            this.hardwareModel = device.getHardware().getModel();
            if (device.getHardware().getRack() != null) {
                this.rackNo = device.getHardware().getRack().getRackNo();
                this.locationName = device.getHardware().getRack().getLocation().getName();
            }
        }
        this.ipAddresses = ipAddresses;
    }
}