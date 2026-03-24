package com.akplaza.infra.domain.device.entity;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ServerSpec {
    private Integer cpuCore;    // 단위: EA
    private Integer memoryGb;   // 단위: GB
}