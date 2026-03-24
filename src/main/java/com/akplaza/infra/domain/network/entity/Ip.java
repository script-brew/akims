// 1-3. Ip.java (개별 IP 자산)
package com.akplaza.infra.domain.network.entity;

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

@Entity
@Table(name = "ip")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ip extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ip_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ip_cidr_id")
    private IpCidr ipCidr;

    @Column(nullable = false, unique = true, length = 45)
    private String ipAddress; // IP 주소 (IPv4, IPv6 호환)

    @Column(nullable = false)
    private boolean isUsed; // 사용 유무 (true: 사용중, false: 미사용)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssignedType assignedType; // 사용 대상 종류

    private Long assignedId; // 사용 대상의 ID (Server ID 또는 NetworkDevice ID)

    @Builder
    public Ip(String ipAddress, boolean isUsed, AssignedType assignedType, Long assignedId) {
        this.ipAddress = ipAddress;
        this.isUsed = isUsed;
        this.assignedType = assignedType != null ? assignedType : AssignedType.NONE;
        this.assignedId = assignedId;
    }

    public void assignCidr(IpCidr ipCidr) {
        this.ipCidr = ipCidr;
    }

    // IP 할당/해제 비즈니스 로직
    public void allocate(AssignedType type, Long targetId) {
        this.isUsed = true;
        this.assignedType = type;
        this.assignedId = targetId;
    }

    public void release() {
        this.isUsed = false;
        this.assignedType = AssignedType.NONE;
        this.assignedId = null;
    }
}