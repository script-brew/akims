package com.akplaza.infra.domain.hardware.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.akplaza.infra.domain.hardware.entity.Hardware;

public interface HardwareRepository extends JpaRepository<Hardware, Long>, JpaSpecificationExecutor<Hardware> {

        boolean existsBySerialNo(String serialNo);

        boolean existsByRackId(Long rackId);

        // 공식: (새 장비의 시작위치 <= 기존 장비의 끝위치) AND (새 장비의 끝위치 >= 기존 장비의 시작위치) 이면 충돌!
        @Query("SELECT CASE WHEN COUNT(h) > 0 THEN true ELSE false END " +
                        "FROM Hardware h " +
                        "WHERE h.rack.id = :rackId " +
                        "AND h.rackPosition <= :endU " +
                        "AND (h.rackPosition + h.size - 1) >= :startU " +
                        "AND (:excludeHardwareId IS NULL OR h.id != :excludeHardwareId)")
        boolean existsOverlappingHardware(@Param("rackId") Long rackId,
                        @Param("startU") Integer startU,
                        @Param("endU") Integer endU,
                        @Param("excludeHardwareId") Long excludeHardwareId);

        // 🌟 랙별 하드웨어 개수를 한 번에 가져오는 최적화 쿼리
        @Query("SELECT h.rack.id, COUNT(h) FROM Hardware h WHERE h.rack IS NOT NULL GROUP BY h.rack.id")
        List<Object[]> countHardwareByRack();
}