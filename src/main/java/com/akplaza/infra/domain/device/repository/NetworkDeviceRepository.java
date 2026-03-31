package com.akplaza.infra.domain.device.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.akplaza.infra.domain.device.entity.NetworkDevice;

public interface NetworkDeviceRepository extends JpaRepository<NetworkDevice, Long> {

    // 장비명(HostName) 중복 검사 및 단건 조회
    boolean existsByName(String name);

    Optional<NetworkDevice> findByName(String name);

    // 🌟 추가: 물리 하드웨어 삭제 전, 해당 하드웨어에 매핑된 서버가 존재하는지 확인
    boolean existsByHardwareId(Long hardwareId);

    // 🌟 성능 최적화: 네트워크 장비 목록을 그릴 때, 연관된 하드웨어와 랙 위치 정보를
    // 여러 번의 쿼리(N+1 문제) 없이 단 한 번의 조인 쿼리로 가져옵니다.
    @Query("SELECT n FROM NetworkDevice n " +
            "LEFT JOIN FETCH n.hardware h " +
            "LEFT JOIN FETCH h.rack r")
    List<NetworkDevice> findAllWithHardwareAndRack();
}