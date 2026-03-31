// 1-2. IpCreateRequest.java (개별 IP 등록 요청)
package com.akplaza.infra.domain.network.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class IpCreateRequest {
    private Long ipCidrId; // 소속될 대역폭 마스터 ID
    private String ipAddress; // 등록할 IP 주소
}