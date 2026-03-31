// 2-2. NetworkDevice.java (네트워크 장비)
package com.akplaza.infra.domain.device.entity;

import com.akplaza.infra.domain.hardware.entity.Hardware;
import com.akplaza.infra.global.common.entity.BaseTimeEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "network_device")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NetworkDevice extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "network_device_id")
    private Long id;

    // 하드웨어 정보 (물리적 제원 및 위치 정보는 Hardware 엔티티에서 관리)
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "hardware_id")
    private Hardware hardware;

    @Column(nullable = false, unique = true, length = 100)
    private String name; // 장비명 (HostName)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NetworkDeviceCategory category;

    @Column(length = 50)
    private String os; // 예: Cisco IOS, FortiOS

    @Lob
    private String description; // 장비 설명

    private boolean ha; // H/A 여부 (Active/Standby 등)

    @Lob
    private String monitoringInfo; // 모니터링 연동 정보

    @Builder
    public NetworkDevice(Hardware hardware, String name, NetworkDeviceCategory category,
            String os, String description, boolean ha, String monitoringInfo) {
        this.hardware = hardware;
        this.name = name;
        this.category = category;
        this.os = os;
        this.description = description;
        this.ha = ha;
        this.monitoringInfo = monitoringInfo;
    }

    public void updateDeviceInfo(String name, NetworkDeviceCategory category,
            String os, String description,
            boolean ha, String monitoringInfo,
            Hardware hardware) {
        this.name = name;
        this.category = category;
        this.os = os;
        this.description = description;
        this.ha = ha;
        this.monitoringInfo = monitoringInfo;
        this.hardware = hardware;
    }
}