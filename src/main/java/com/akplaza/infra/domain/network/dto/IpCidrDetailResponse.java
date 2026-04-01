package com.akplaza.infra.domain.network.dto;

import java.util.List;

import com.akplaza.infra.domain.network.entity.IpCidr;

import lombok.Getter;

@Getter
public class IpCidrDetailResponse {
    private Long id;
    private String cidrBlock;
    private String description;
    // 🌟 이 대역에 속한 개별 IP들의 현황 목록 (툴팁 및 색상 표시에 사용)
    private List<IpResponse> ips;

    public IpCidrDetailResponse(IpCidr ipCidr, List<IpResponse> ips) {
        this.id = ipCidr.getId();
        this.cidrBlock = ipCidr.getCidrBlock();
        this.description = ipCidr.getDescription();
        this.ips = ips;
    }
}