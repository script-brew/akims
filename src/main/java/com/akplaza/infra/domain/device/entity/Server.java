package com.akplaza.infra.domain.device.entity;

import java.util.ArrayList;
import java.util.List;

import com.akplaza.infra.domain.hardware.entity.Hardware;
import com.akplaza.infra.domain.software.entity.Software;
import com.akplaza.infra.global.common.entity.BaseTimeEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "server")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Server extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "server_id")
    private Long id;

    // A server is a specific piece of hardware.
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "hardware_id")
    private Hardware hardware;

    @Column(nullable = false, unique = true, length = 100)
    private String name; // 서버명 (hostname)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ServerCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Environment environment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ServerType serverType;

    @Column(length = 50)
    private String os; // 예: RHEL 8.4, Windows Server 2022

    @Lob
    private String description;

    @Embedded
    private ServerSpec spec;

    // One server can have many disks
    @OneToMany(mappedBy = "server", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Disk> disks = new ArrayList<>();

    // One server can have many installed software
    @OneToMany(mappedBy = "server", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Software> installedSoftware = new ArrayList<>();

    private boolean ha; // H/A 여부

    @Lob
    private String backupInfo;

    @Lob
    private String monitoringInfo;

    // IP relationship will be added later when the IP entity is created.

    @Builder
    public Server(Hardware hardware, String name, ServerCategory category, Environment environment,
            ServerType serverType, String os, String description, ServerSpec spec,
            boolean ha, String backupInfo, String monitoringInfo) {
        this.hardware = hardware;
        this.name = name;
        this.category = category;
        this.environment = environment;
        this.serverType = serverType;
        this.os = os;
        this.description = description;
        this.spec = spec;
        this.ha = ha;
        this.backupInfo = backupInfo;
        this.monitoringInfo = monitoringInfo;
    }

    // Helper methods to manage relationships
    public void addDisk(Disk disk) {
        this.disks.add(disk);
        disk.setServer(this);
    }

    public void removeDisk(Disk disk) {
        this.disks.remove(disk);
        disk.setServer(null);
    }

    public void addSoftware(Software software) {
        this.installedSoftware.add(software);
        software.setServer(this);
    }

    public void removeSoftware(Software software) {
        this.installedSoftware.remove(software);
        software.setServer(null);
    }

    public void scaleUpSpec(ServerSpec newSpec) {
        this.spec = newSpec;
    }

    public Integer getTotalDiskSizeGb() {
        return this.disks.stream()
                .mapToInt(Disk::getSize)
                .sum();
    }
}