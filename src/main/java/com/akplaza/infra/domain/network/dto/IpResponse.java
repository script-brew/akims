// 1-3. IpResponse.java (IP 목록/단건 조회 응답)
package com.akplaza.infra.domain.network.dto;

import com.akplaza.infra.domain.network.entity.Ip;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IpResponse {
    private Long id;
    private String ipAddress;

    @JsonProperty("isUsed")
    private boolean isUsed;
    private String assignedType;
    private Long assignedId;
    private String assignedTargetName;

    // 조인된 CIDR 정보
    private Long ipCidrId;
    private String cidrBlock;

    public IpResponse(Ip ip) {
        this.id = ip.getId();
        this.ipAddress = ip.getIpAddress();
        this.isUsed = ip.isUsed();
        this.assignedType = ip.getAssignedType() != null ? ip.getAssignedType().name() : "NONE";
        this.assignedId = ip.getAssignedId();

        if (ip.getIpCidr() != null) {
            this.ipCidrId = ip.getIpCidr().getId();
            this.cidrBlock = ip.getIpCidr().getCidrBlock();
        }
    }
}