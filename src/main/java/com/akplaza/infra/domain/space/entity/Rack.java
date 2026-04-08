package com.akplaza.infra.domain.space.entity;

import com.akplaza.infra.global.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(name = "rack")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Rack extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rack_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Column(nullable = false, length = 50)
    private String rackNo; // 랙 번호

    @Column(nullable = false, length = 100)
    private String name; // 랙 이름 (예: RACK-A01, RACK-B15)

    @Column(nullable = false)
    private Integer size; // 총 U 수 (보통 42U 또는 48U)

    @Column
    private String description;

    public void updateRackInfo(Location location, String rackNo, String name, Integer size, String description) {
        this.location = location;
        this.rackNo = rackNo;
        this.name = name;
        this.size = size;
        this.description = description;
    }

    @Builder
    public Rack(Location location, String rackNo, String name, Integer size, String description) {
        this.location = location;
        this.rackNo = rackNo;
        this.name = name;
        this.size = size != null ? size : 42; // 기본값 42U
        this.description = description;
    }
}