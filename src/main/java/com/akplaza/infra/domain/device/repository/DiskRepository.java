package com.akplaza.infra.domain.device.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.akplaza.infra.domain.device.entity.Disk;

public interface DiskRepository extends JpaRepository<Disk, Long> {
    // 특정 서버에 마운트된 디스크 목록 조회
    List<Disk> findByServerId(Long serverId);
}
