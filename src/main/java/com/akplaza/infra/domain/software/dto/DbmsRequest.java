// DbmsRequest.java
package com.akplaza.infra.domain.software.dto;

import lombok.Data;

@Data
public class DbmsRequest {
    private String name;
    private String version;
    private String description;
}