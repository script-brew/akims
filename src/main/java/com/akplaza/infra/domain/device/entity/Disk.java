package com.akplaza.infra.domain.device.entity;

import com.akplaza.infra.global.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "disk")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Disk extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "disk_id")
    private Long id;

    // A disk belongs to a server
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id")
    @Setter // Needed for the bidirectional relationship management
    private Server server;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(50)")
    private DiskType diskType; // 디스크 타입

    @Column(nullable = false)
    private Integer size; // 단위: GB

    @Column(nullable = false, length = 50)
    private String mountPoint; // 예: "C:\", "/", "/data"

    @Builder
    public Disk(Server server, DiskType diskType, Integer size, String mountPoint) {
        this.server = server;
        this.diskType = diskType;
        this.size = size;
        this.mountPoint = mountPoint;
    }

    public void updateDiskInfo(DiskType diskType, Integer size, String mountPoint) {
        this.diskType = diskType;
        this.size = size;
        this.mountPoint = mountPoint;
    }
}