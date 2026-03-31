package com.akplaza.infra.domain.device.dto;

import java.util.List;

import com.akplaza.infra.domain.device.entity.Server;

import lombok.Getter;

@Getter
public class ServerResponse {
    private Long id;
    private String name;
    private String category;
    private String environment;
    private String serverType;
    private String platform;
    private String os;
    private Integer cpuCore;
    private Integer memoryGb;
    private boolean ha;

    // 하드웨어 정보 (조인된 데이터)
    private Long hardwareId;
    private String hardwareModel;
    private String locationName; // 장소명
    private String rackNo; // 랙 번호

    // IP 목록 (IpService 등을 통해 주입)
    private List<String> ipAddresses;

    // 엔티티를 DTO로 변환하는 생성자
    public ServerResponse(Server server, List<String> ipAddresses) {
        this.id = server.getId();
        this.name = server.getName();
        this.category = server.getCategory().name();
        this.environment = server.getEnvironment().name();
        this.serverType = server.getServerType().name();
        this.platform = server.getPlatform();
        this.os = server.getOs();
        this.cpuCore = server.getSpec() != null ? server.getSpec().getCpuCore() : 0;
        this.memoryGb = server.getSpec() != null ? server.getSpec().getMemoryGb() : 0;
        this.ha = server.isHa();

        // 연관된 하드웨어가 있는 경우 정보 추출 (Null-safe 처리)
        if (server.getHardware() != null) {
            this.hardwareId = server.getHardware().getId();
            this.hardwareModel = server.getHardware().getModel();
            if (server.getHardware().getRack() != null) {
                this.rackNo = server.getHardware().getRack().getRackNo();
                this.locationName = server.getHardware().getRack().getLocation().getName();
            }
        }
        this.ipAddresses = ipAddresses;
    }
}