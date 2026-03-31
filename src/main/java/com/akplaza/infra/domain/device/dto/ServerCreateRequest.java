package com.akplaza.infra.domain.device.dto;

import java.util.ArrayList;
import java.util.List;

import com.akplaza.infra.domain.device.entity.Environment;
import com.akplaza.infra.domain.device.entity.ServerCategory;
import com.akplaza.infra.domain.device.entity.ServerType;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ServerCreateRequest {
    private String hostName;
    private ServerCategory category;
    private Environment environment;
    private ServerType serverType;
    private String os;
    private String description;

    // ServerSpec 임베디드 매핑용
    private Integer cpuCore;
    private Integer memoryGb;

    private boolean ha;
    private String backupInfo;
    private String monitoringInfo;

    private Long hardwareId; // 물리 서버일 경우 매핑할 하드웨어 ID

    private List<String> ipAddresses = new ArrayList<>(); // 할당할 IP 목록
}