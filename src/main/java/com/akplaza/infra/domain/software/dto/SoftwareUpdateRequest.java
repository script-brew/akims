// 3-2. SoftwareUpdateRequest.java
package com.akplaza.infra.domain.software.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SoftwareUpdateRequest {

    @NotBlank(message = "변경할 소프트웨어 이름은 필수 입력값입니다.")
    private String name;

    private String version;
    private String purpose;
    private String maintenanceInfo;
}