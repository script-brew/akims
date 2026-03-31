// 3-3. SoftwareResponse.java
package com.akplaza.infra.domain.software.dto;

import com.akplaza.infra.domain.software.entity.Software;

import lombok.Getter;

@Getter
public class SoftwareResponse {
    private Long id;
    private String name;
    private String version;
    private String purpose;
    private String maintenanceInfo;

    // 조인된 서버 정보
    private Long serverId;
    private String serverName;

    public SoftwareResponse(Software software) {
        this.id = software.getId();
        this.name = software.getName();
        this.version = software.getVersion();
        this.purpose = software.getPurpose();
        this.maintenanceInfo = software.getMaintenanceInfo();

        if (software.getServer() != null) {
            this.serverId = software.getServer().getId();
            this.serverName = software.getServer().getName();
        }
    }
}