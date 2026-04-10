// 3-3. DiskResponse.java
package com.akplaza.infra.domain.device.dto;

import com.akplaza.infra.domain.device.entity.Disk;

import lombok.Getter;

@Getter
public class DiskResponse {
    private Long id;
    private String diskType;
    private Integer size;
    private String diskName;

    // 조인된 서버 정보
    private Long serverId;
    private String serverName;

    public DiskResponse(Disk disk) {
        this.id = disk.getId();
        this.diskType = disk.getDiskType().name();
        this.size = disk.getSize();
        this.diskName = disk.getDiskName();

        if (disk.getServer() != null) {
            this.serverId = disk.getServer().getId();
            this.serverName = disk.getServer().getHostName();
        }
    }
}