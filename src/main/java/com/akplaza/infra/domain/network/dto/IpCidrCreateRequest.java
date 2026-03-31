// 1-1. IpCidrCreateRequest.java (IP 대역 생성 요청)
package com.akplaza.infra.domain.network.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class IpCidrCreateRequest {

    @NotBlank(message = "IP 대역(CIDR Block) 정보는 필수입니다. (예: 192.168.1.0/24)")
    private String cidrBlock; // 예: "192.168.10.0/24"

    @NotBlank(message = "해당 대역의 용도(설명)를 입력해주세요.")
    private String description; // 예: "운영 WEB 서버 대역"
}