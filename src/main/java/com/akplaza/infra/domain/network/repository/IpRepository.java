package com.akplaza.infra.domain.network.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.akplaza.infra.domain.network.entity.AssignedType;
import com.akplaza.infra.domain.network.entity.Ip;

public interface IpRepository extends JpaRepository<Ip, Long> {

    // IP 주소 자체로 단건 조회 (예: 192.168.1.100)
    Optional<Ip> findByIpAddress(String ipAddress);

    // 🌟 핵심: 특정 장비(서버 또는 네트워크 장비)에 할당된 IP 목록 조회
    // 예: findByAssignedTypeAndAssignedId(AssignedType.SERVER, 1L)
    List<Ip> findByAssignedTypeAndAssignedId(AssignedType assignedType, Long assignedId);

    // 특정 CIDR 대역(예: DMZ 대역)에 속한 모든 IP 목록 조회
    List<Ip> findByIpCidrId(Long ipCidrId);

    // IP 중복 검사
    boolean existsByIpAddress(String ipAddress);

    // IP 대역 삭제
    void deleteByIpCidrId(Long ipCidrId);

}