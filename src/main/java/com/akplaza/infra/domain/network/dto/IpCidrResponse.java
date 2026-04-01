package com.akplaza.infra.domain.network.dto;

import com.akplaza.infra.domain.network.entity.IpCidr;

import lombok.Getter;

@Getter
public class IpCidrResponse {
    private Long id;
    private String cidrBlock; // 예: 10.10.10.0/24
    private String description;

    public IpCidrResponse(IpCidr ipCidr) {
        this.id = ipCidr.getId();
        this.cidrBlock = ipCidr.getCidrBlock();
        this.description = ipCidr.getDescription();
    }
}