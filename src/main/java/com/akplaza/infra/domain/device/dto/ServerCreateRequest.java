package com.akplaza.infra.domain.device.dto;

import java.util.List;

import com.akplaza.infra.domain.device.entity.Environment;
import com.akplaza.infra.domain.device.entity.ServerCategory;
import com.akplaza.infra.domain.device.entity.ServerBackup;
import com.akplaza.infra.domain.device.entity.ServerMonitoring;
import com.akplaza.infra.domain.device.entity.ServerType;
import com.akplaza.infra.domain.network.dto.IpAssignRequest;
import com.akplaza.infra.domain.software.dto.SoftwareRequest;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class ServerCreateRequest {

    @NotBlank(message = "서버명(HostName)은 필수입니다.")
    private String hostName;

    @NotNull(message = "서버 카테고리를 선택해주세요.")
    private ServerCategory serverCategory;

    @NotNull(message = "운영 환경(Environment)을 선택해주세요.")
    private Environment environment;

    @NotNull(message = "서버 타입(물리/가상)을 선택해주세요.")
    private ServerType serverType;

    private String platform; // 클라우드가 아닐 수 있으므로 NotBlank 적용 안함

    @NotBlank(message = "운영체제(OS) 정보는 필수입니다.")
    private String os;

    private String description;

    // ServerSpec 임베디드 매핑용
    @NotNull(message = "CPU Core 수를 입력해주세요.")
    @DecimalMin(value = "0.1", message = "CPU Core는 0.1 이상이어야 합니다.")
    private Double cpuCore;

    @NotNull(message = "Memory 용량을 입력해주세요.")
    @DecimalMin(value = "0.5", message = "Memory는 0.5GB 이상이어야 합니다.")
    private Double memoryGb;

    private boolean ha;
    private ServerBackup backupInfo;
    private ServerMonitoring monitoringInfo;

    private Long hardwareId; // 물리 서버일 경우 매핑할 하드웨어 ID

    private List<IpAssignRequest> ips; // 할당할 IP 목록
    private List<DiskRequest> disks;
    private List<SoftwareRequest> softwares;

}