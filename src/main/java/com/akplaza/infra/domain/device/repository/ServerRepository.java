package com.akplaza.infra.domain.device.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import com.akplaza.infra.domain.device.entity.Server;
import com.akplaza.infra.domain.hardware.entity.Hardware;

public interface ServerRepository extends JpaRepository<Server, Long>, JpaSpecificationExecutor<Server> {
    // 향후 호스트명 중복 검사 등의 메서드를 여기에 추가할 수 있습니다.
    boolean existsByHostName(String hostName);

    // 🌟 추가: 물리 하드웨어 삭제 전, 해당 하드웨어에 매핑된 서버가 존재하는지 확인
    boolean existsByHardwareId(Long hardwareId);

    // 🌟 성능 최적화: 서버, 하드웨어, 랙 정보를 한 번의 SQL 조인으로 가져옵니다.
    @Query("SELECT s FROM Server s " +
            "LEFT JOIN FETCH s.hardware h " +
            "LEFT JOIN FETCH h.rack r")
    List<Server> findAllWithHardwareAndRack();

    @Query("SELECT s.os, COUNT(s) FROM Server s GROUP BY s.os")
    java.util.List<Object[]> countByOs();

    @Query("SELECT s.environment, COUNT(s) FROM Server s GROUP BY s.environment")
    java.util.List<Object[]> countByEnvironment();

}