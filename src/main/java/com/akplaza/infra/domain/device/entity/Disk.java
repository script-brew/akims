package com.akplaza.infra.domain.device.entity;

import com.akplaza.infra.global.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

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
    @Column(nullable = false, length = 20)
    private DiskType type;  // 디스크 타입

    @Column(nullable = false)
    private Integer size;     // 단위: GB

    @Column(nullable = false, length = 50)
    private String mountPoint;  // 예: "C:\", "/", "/data"

    @Builder
    public Disk(DiskType type, Integer size, String mountPoint) {
        this.type = type;
        this.size = size;
        this.mountPoint = mountPoint;
    }
}