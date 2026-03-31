package com.akplaza.infra.domain.space.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RackRequest {
    private Long locationId;
    private String rackNo;
    private String name;
    private Integer size;
}
