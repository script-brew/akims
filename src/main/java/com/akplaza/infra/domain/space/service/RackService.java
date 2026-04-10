package com.akplaza.infra.domain.space.service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.akplaza.infra.domain.hardware.repository.HardwareRepository;
import com.akplaza.infra.domain.space.dto.RackRequest;
import com.akplaza.infra.domain.space.dto.RackResponse;
import com.akplaza.infra.domain.space.entity.Location;
import com.akplaza.infra.domain.space.entity.Rack;
import com.akplaza.infra.domain.space.repository.LocationRepository;
import com.akplaza.infra.domain.space.repository.RackRepository;
import com.akplaza.infra.global.common.repository.DynamicSearchSpec;
import com.akplaza.infra.global.error.exception.DuplicateResourceException;
import com.akplaza.infra.global.error.exception.ResourceInUseException;
import com.akplaza.infra.global.error.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RackService {
    private final RackRepository rackRepository;
    private final LocationRepository locationRepository;
    private final HardwareRepository hardwareRepository;

    @Transactional
    public Long createRack(RackRequest dto) {
        log.info("[CREATE] Rack 등록 요청: {}", dto.getRackNo());
        if (rackRepository.existsByRackNo(dto.getRackNo())) {
            throw new DuplicateResourceException("이미 존재하는 랙 번호입니다.");
        }
        Location location = locationRepository.findById(dto.getLocationId())
                .orElseThrow(() -> new ResourceNotFoundException("장소 정보를 찾을 수 없습니다."));

        Rack rack = Rack.builder()
                .location(location)
                .rackNo(dto.getRackNo())
                .name(dto.getName())
                .size(dto.getSize())
                .rackType(dto.getRackType())
                .description(dto.getDescription())
                .build();
        return rackRepository.save(rack).getId();
    }

    @Transactional
    public void deleteRack(Long id) {
        log.info("[DELETE] Rack 삭제 요청 - ID: {}", id);
        if (hardwareRepository.existsByRackId(id)) {
            log.error("삭제 실패 - 해당 랙에 실장된 하드웨어가 존재함. ID: {}", id);
            throw new ResourceInUseException("실장된 장비가 있어 삭제할 수 없습니다.");
        }
        rackRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Page<RackResponse> searchServers(Map<String, String> searchParams, Pageable pageable) {

        // 1. 공통 유틸을 통해 동적 WHERE 절(Specification) 생성
        Specification<Rack> spec = DynamicSearchSpec.searchConditions(searchParams);

        // 2. Repository의 findAll(spec, pageable) 호출하여 기본 서버 목록 조회
        Page<Rack> rackPage = rackRepository.findAll(spec, pageable);
        List<Rack> racks = rackPage.getContent();

        // 결과가 없으면 빈 페이지 반환
        if (racks.isEmpty()) {
            return new PageImpl<>(java.util.Collections.emptyList(), pageable, rackPage.getTotalElements());
        }

        // 3. Rack별 하드웨어 수 조회
        List<Object[]> counts = hardwareRepository.countHardwareByRack();
        Map<Long, Integer> countMap = counts.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Number) row[1]).intValue()));

        // 5. 🌟 DTO로 안전하게 조립 (데이터 중복 증식 방지)
        List<RackResponse> responseList = racks.stream().map(rack -> {
            return new RackResponse(rack, countMap.getOrDefault(rack.getId(), 0));
        }).collect(Collectors.toList());

        // 6. 최종 Page 객체 반환
        return new PageImpl<>(responseList, pageable, rackPage.getTotalElements());
    }

    public List<RackResponse> getAll() {
        List<Rack> racks = rackRepository.findAll();
        // 🌟 N+1 쿼리 방어: 랙별 하드웨어 개수를 DB에서 단 1번의 쿼리로 가져와 매핑
        List<Object[]> counts = hardwareRepository.countHardwareByRack();
        Map<Long, Integer> countMap = counts.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Number) row[1]).intValue()));

        return racks.stream()
                // Map에서 개수를 꺼내 DTO에 전달 (없으면 0)
                .map(rack -> new RackResponse(rack, countMap.getOrDefault(rack.getId(), 0)))
                .collect(Collectors.toList());
    }

    @Transactional
    public Long updateRack(Long id, RackRequest dto) {
        log.info("[UPDATE] Rack 수정 요청: {}", dto.getRackNo());

        Rack rack = rackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("수정할 랙을 찾을 수 없습니다. ID: " + id));

        Location location = locationRepository.findById(dto.getLocationId())
                .orElseThrow(() -> new ResourceNotFoundException("장소 정보를 찾을 수 없습니다."));

        rack.updateRackInfo(location, dto.getRackNo(), dto.getName(), dto.getSize(), dto.getDescription(),
                dto.getRackType());
        return rackRepository.save(rack).getId();
    }
}
