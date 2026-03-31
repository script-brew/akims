package com.akplaza.infra.domain.device.dto;

import com.akplaza.infra.domain.device.entity.Environment;
import com.akplaza.infra.domain.device.entity.ServerCategory;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ServerUpdateRequest {
    // 식별자(HostName)나 ServerType은 쉽게 변경되지 않으므로 수정 항목에서 제외하거나 엄격히 관리
    private String hostName;
    private ServerCategory category;
    private Environment environment;
    private String os;
    private String description;
    private Integer cpuCore;
    private Integer memoryGb;
    private boolean ha;
    private String backupInfo;
    private String monitoringInfo;
    private Long hardwareId; // 하드웨어 교체 시 사용
}