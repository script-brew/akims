package com.akplaza.infra.domain.device.dto;

import java.util.List;

import com.akplaza.infra.domain.device.entity.Environment;
import com.akplaza.infra.domain.device.entity.ServerCategory;
import com.akplaza.infra.domain.network.dto.IpAssignRequest;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ServerUpdateRequest {
    // 식별자(HostName)나 ServerType은 쉽게 변경되지 않으므로 수정 항목에서 제외하거나 엄격히 관리
    @NotBlank(message = "변경할 서버명(HostName)은 필수 입력값입니다.")
    private String hostName;

    @NotNull(message = "서버 카테고리는 필수입니다.")
    private ServerCategory serverCategory;

    @NotNull(message = "운영 환경(Environment)은 필수입니다.")
    private Environment environment;

    @NotBlank(message = "운영체제(OS) 정보는 필수입니다.")
    private String os;

    private String description;

    @NotNull(message = "CPU Core 수를 입력해주세요.")
    @Min(value = 1, message = "CPU Core는 1 이상이어야 합니다.")
    private Integer cpuCore;

    @NotNull(message = "Memory 용량을 입력해주세요.")
    @Min(value = 1, message = "Memory는 1GB 이상이어야 합니다.")
    private Integer memoryGb;

    private boolean ha;
    private String backupInfo;
    private String monitoringInfo;

    private Long hardwareId; // 하드웨어 교체 시 사용

    private List<IpAssignRequest> ips; // 할당할 IP 목록 (VIP, 관리 IP 등)
}