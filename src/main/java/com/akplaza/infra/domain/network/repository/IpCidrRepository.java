package com.akplaza.infra.domain.network.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import com.akplaza.infra.domain.network.entity.IpCidr;

public interface IpCidrRepository extends JpaRepository<IpCidr, Long>, JpaSpecificationExecutor<IpCidr> {

    // 특정 CIDR 대역 문자열로 대역 마스터 정보 조회
    Optional<IpCidr> findByCidrBlock(String cidrBlock);

    // CIDR 대역 중복 등록 방지를 위한 검증
    boolean existsByCidrBlock(String cidrBlock);
}