package com.akplaza.infra.domain.space.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.akplaza.infra.domain.space.entity.Location;

public interface LocationRepository extends JpaRepository<Location, Long> {

    // 장비명(HostName) 중복 검사 및 단건 조회
    boolean existsByName(String name);
}