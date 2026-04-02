package com.akplaza.infra.domain.network.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IpAssignRequest {
    private Long ipCidrId;
    private String ipAddress;
}