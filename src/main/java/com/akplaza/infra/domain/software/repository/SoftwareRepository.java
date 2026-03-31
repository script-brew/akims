package com.akplaza.infra.domain.software.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.akplaza.infra.domain.software.entity.Software;

public interface SoftwareRepository extends JpaRepository<Software, Long> {

    // 특정 서버에 설치된 소프트웨어 목록 조회
    List<Software> findByServerId(Long serverId);
}