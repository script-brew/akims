package com.akplaza.infra.domain.device.dto;

import com.akplaza.infra.domain.device.entity.DiskType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiskRequest {
    private DiskType diskType; // SSD, HDD, NVME, SAN 등
    private int size; // 용량 (GB)
    private String diskName; // 용도/이름 (예: "/", "/data", "C:\")
}