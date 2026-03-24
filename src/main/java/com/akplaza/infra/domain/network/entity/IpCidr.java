// 1-2. IpCidr.java (IP 대역 마스터)
package com.akplaza.infra.domain.network.entity;

import java.util.ArrayList;
import java.util.List;

import com.akplaza.infra.global.common.entity.BaseTimeEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ip_cidr")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IpCidr extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ip_cidr_id")
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String cidrBlock; // 예: "192.168.10.0/24"

    @Lob
    private String description; // 용도 설명 (예: "DMZ 웹서버 대역")

    // 하나의 대역에는 여러 IP가 속함
    @OneToMany(mappedBy = "ipCidr", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Ip> ips = new ArrayList<>();

    @Builder
    public IpCidr(String cidrBlock, String description) {
        this.cidrBlock = cidrBlock;
        this.description = description;
    }

    public void addIp(Ip ip) {
        this.ips.add(ip);
        ip.assignCidr(this);
    }
}