// 1-3. NetworkDeviceResponse.java (조회 응답 DTO)
package com.akplaza.infra.domain.device.dto;

import java.util.List;
import java.util.stream.Collectors;

import com.akplaza.infra.domain.device.entity.NetworkDevice;
import com.akplaza.infra.domain.network.dto.IpAssignRequest;
import com.akplaza.infra.domain.network.entity.Ip;

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
    private String serialNo;
    private String locationName;
    private String rackNo;

    // 할당된 IP 목록
    private List<IpAssignRequest> ips;

    public NetworkDeviceResponse(NetworkDevice device, List<Ip> assignedIps) {
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
            this.serialNo = device.getHardware().getSerialNo();
            if (device.getHardware().getRack() != null) {
                this.rackNo = device.getHardware().getRack().getRackNo();
                this.locationName = device.getHardware().getRack().getLocation().getName();
            }
        }
        // 🌟 여러 개의 Ip 엔티티를 DTO 리스트로 변환
        if (assignedIps != null) {
            this.ips = assignedIps.stream()
                    .map(ip -> new IpAssignRequest(ip.getIpCidr().getId(), ip.getIpAddress()))
                    .collect(Collectors.toList());
        }
    }
}