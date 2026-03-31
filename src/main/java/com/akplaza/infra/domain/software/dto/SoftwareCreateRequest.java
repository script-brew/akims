package com.akplaza.infra.domain.software.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SoftwareCreateRequest {

    @NotNull(message = "설치될 대상 서버 ID는 필수입니다.")
    private Long serverId;

    @NotBlank(message = "소프트웨어 이름은 필수 입력값입니다.")
    private String name;

    private String version;
    private String purpose;
    private String maintenanceInfo;
}