package com.akplaza.infra.domain.hardware.entity;

import com.akplaza.infra.domain.space.entity.Rack;
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
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "hardware")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 캡슐화 보호
public class Hardware extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hardware_id")
    private Long id;

    // 하드웨어는 특정 랙에 실장됩니다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rack_id")
    private Rack rack;

    // --- 랙실장도 (Rack Diagram) 표현을 위한 위치 정보 ---
    @Column(nullable = false)
    private Integer rackPosition; // 실장 위치 (예: 아래에서부터 15번 U에 장착)

    @Column(nullable = false)
    private Integer size; // 장비 크기 (예: 2U 크기의 장비)

    // --- 하드웨어 기본 제원 ---
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(50)")
    private EquipmentType equipmentType; // 장비 종류 (SERVER, NETWORK 등)

    private Integer introductionYear; // 도입년도 (예: 2026)

    @Column(nullable = false, length = 100)
    private String model; // 모델명 (예: PowerEdge R740)

    @Column(nullable = false, unique = true, length = 100)
    private String serialNo; // 시리얼 넘버 (고유값)

    // 🌟 추가: 단일 전원 여부 (true: 단일 전원, false/null: 이중 전원 이상)
    @Column(name = "is_single_power")
    private Boolean isSinglePower;
    private String powerLine;

    @Lob // 설명이 길어질 수 있으므로 Large Object 타입으로 지정
    private String description; // 장비 설명 및 비고

    // 위치 이동이나 정보 수정은 Entity 내부에서 처리
    public void moveRackLocation(Rack newRack, Integer newRackPosition) {
        this.rack = newRack;
        this.rackPosition = newRackPosition;
    }

    public void updateHardwareInfo(String model, String serialNo, EquipmentType equipmentType,
            Integer introductionYear, Integer size, String description,
            Rack rack, Integer rackPosition, Boolean isSinglePower, String powerLine) {
        this.model = model;
        this.serialNo = serialNo;
        this.equipmentType = equipmentType;
        this.introductionYear = introductionYear;
        this.size = size;
        this.description = description;
        this.rack = rack;
        this.rackPosition = rackPosition;
        this.isSinglePower = isSinglePower;
        this.powerLine = powerLine;
    }

    @Builder
    public Hardware(Rack rack, Integer rackPosition, Integer size, EquipmentType equipmentType,
            Integer introductionYear, String model, String serialNo, String description, Boolean isSinglePower,
            String powerLine) {
        this.rack = rack;
        this.rackPosition = rackPosition;
        this.size = size != null ? size : 1; // 기본 1U
        this.equipmentType = equipmentType;
        this.introductionYear = introductionYear;
        this.model = model;
        this.serialNo = serialNo;
        this.description = description;
        this.isSinglePower = isSinglePower;
        this.powerLine = powerLine;
    }
}