// SoftwareRequest.java
package com.akplaza.infra.domain.software.dto;

import lombok.Data;

@Data
public class SoftwareRequest {
    private String name;
    private String version;
    private String purpose;
    private String maintenanceInfo;
}