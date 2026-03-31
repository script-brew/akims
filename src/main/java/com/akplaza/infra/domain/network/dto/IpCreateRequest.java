// 1-2. IpCreateRequest.java (개별 IP 등록 요청)
package com.akplaza.infra.domain.network.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class IpCreateRequest {

    @NotNull(message = "소속될 IP 대역(CIDR) ID는 필수입니다.")
    private Long ipCidrId; // 소속될 대역폭 마스터 ID

    @NotBlank(message = "등록할 IP 주소는 필수입니다.")
    private String ipAddress; // 등록할 IP 주소
}