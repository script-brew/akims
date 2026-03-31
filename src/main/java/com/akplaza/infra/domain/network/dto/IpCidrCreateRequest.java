// 1-1. IpCidrCreateRequest.java (IP 대역 생성 요청)
package com.akplaza.infra.domain.network.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class IpCidrCreateRequest {
    private String cidrBlock; // 예: "192.168.10.0/24"
    private String description; // 예: "운영 WEB 서버 대역"
}