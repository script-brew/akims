// 3-1. DiskCreateRequest.java
package com.akplaza.infra.domain.device.dto;

import com.akplaza.infra.domain.device.entity.DiskType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DiskCreateRequest {

    @NotNull(message = "디스크를 장착할 서버 ID는 필수입니다.")
    private Long serverId;

    @NotNull(message = "디스크 타입(HDD, SSD, NVMe 등)을 선택해주세요.")
    private DiskType type;

    @NotNull(message = "디스크 용량을 입력해주세요.")
    @Min(value = 1, message = "디스크 용량은 1GB 이상이어야 합니다.")
    private Integer size;

    @NotBlank(message = "마운트 포인트(예: /, C:\\)는 필수 입력값입니다.")
    private String mountPoint;
}