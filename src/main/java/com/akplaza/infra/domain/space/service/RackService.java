package com.akplaza.infra.domain.space.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.akplaza.infra.domain.hardware.repository.HardwareRepository;
import com.akplaza.infra.domain.space.dto.RackRequest;
import com.akplaza.infra.domain.space.dto.RackResponse;
import com.akplaza.infra.domain.space.entity.Location;
import com.akplaza.infra.domain.space.entity.Rack;
import com.akplaza.infra.domain.space.repository.LocationRepository;
import com.akplaza.infra.domain.space.repository.RackRepository;
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

    public List<RackResponse> getAll() {
        return rackRepository.findAll().stream().map(RackResponse::new).toList();
    }
}
