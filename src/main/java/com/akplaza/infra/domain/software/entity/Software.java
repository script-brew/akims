package com.akplaza.infra.domain.software.entity;

import com.akplaza.infra.domain.device.entity.Server;
import com.akplaza.infra.global.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "software")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 캡슐화
public class Software extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "software_id")
    private Long id;

    @Column(nullable = false, length = 100)
    private String name; // 소프트웨어명 (예: Nginx, Apache Tomcat)

    @Column(length = 50)
    private String version; // 🌟 추가: 버전 정보

    @Column(length = 100)
    private String purpose; // 🌟 추가: 설치 용도

    @Lob
    private String maintenanceInfo; // 🌟 추가: 유지보수 계약 정보 및 담당자

    // 연관관계의 주인
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id")
    @Setter
    private Server server;

    @Builder
    public Software(String name, String version, String purpose, String maintenanceInfo) {
        this.name = name;
        this.version = version;
        this.purpose = purpose;
        this.maintenanceInfo = maintenanceInfo;
    }
}