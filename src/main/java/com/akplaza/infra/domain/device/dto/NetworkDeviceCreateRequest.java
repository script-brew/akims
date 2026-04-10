// 1-1. NetworkDeviceCreateRequest.java (등록 요청 DTO)
package com.akplaza.infra.domain.device.dto;

import java.util.List;

import com.akplaza.infra.domain.device.entity.NetworkDeviceCategory;
import com.akplaza.infra.domain.network.dto.IpAssignRequest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class NetworkDeviceCreateRequest {

    @NotBlank(message = "장비명(HostName)은 필수 입력값입니다.")
    private String name; // 장비명 (HostName)

    @NotNull(message = "장비 분류(Category)를 선택해주세요.")
    private NetworkDeviceCategory category; // L2, L3, FIREWALL 등

    @NotBlank(message = "운영체제(OS) 정보는 필수입니다.")
    private String os;

    private String description;
    private boolean ha;
    private String monitoringInfo;

    private Long hardwareId; // 물리 하드웨어 매핑 ID (가상 어플라이언스인 경우 null 허용)

    private List<IpAssignRequest> ips; // 할당할 IP 목록 (VIP, 관리 IP 등)
}