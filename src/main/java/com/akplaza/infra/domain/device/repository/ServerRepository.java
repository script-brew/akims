package com.akplaza.infra.domain.device.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.akplaza.infra.domain.device.entity.Server;

public interface ServerRepository extends JpaRepository<Server, Long> {
    // 향후 호스트명 중복 검사 등의 메서드를 여기에 추가할 수 있습니다.
    boolean existsByHostName(String hostName);

    // 🌟 성능 최적화: 서버, 하드웨어, 랙 정보를 한 번의 SQL 조인으로 가져옵니다.
    @Query("SELECT s FROM Server s " +
            "LEFT JOIN FETCH s.hardware h " +
            "LEFT JOIN FETCH h.rack r")
    List<Server> findAllWithHardwareAndRack();
}