package com.akplaza.infra.domain.device.dto;

import java.util.ArrayList;
import java.util.List;

import com.akplaza.infra.domain.device.entity.Environment;
import com.akplaza.infra.domain.device.entity.ServerCategory;
import com.akplaza.infra.domain.device.entity.ServerType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
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
    @Min(value = 1, message = "CPU Core는 1 이상이어야 합니다.")
    private Integer cpuCore;

    @NotNull(message = "Memory 용량을 입력해주세요.")
    @Min(value = 1, message = "Memory는 1GB 이상이어야 합니다.")
    private Integer memoryGb;

    private boolean ha;
    private String backupInfo;
    private String monitoringInfo;

    private Long hardwareId; // 물리 서버일 경우 매핑할 하드웨어 ID

    private List<String> ipAddresses = new ArrayList<>(); // 할당할 IP 목록
}