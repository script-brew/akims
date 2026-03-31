package com.akplaza.infra.domain.space.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.akplaza.infra.domain.space.dto.LocationRequest;
import com.akplaza.infra.domain.space.dto.LocationResponse;
import com.akplaza.infra.domain.space.entity.Location;
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
public class LocationService {
    private final LocationRepository locationRepository;
    private final RackRepository rackRepository;

    @Transactional
    public Long createLocation(LocationRequest dto) {
        log.info("[CREATE] Location 등록 요청: {}", dto.getName());
        if (locationRepository.existsByName(dto.getName())) {
            throw new DuplicateResourceException("이미 존재하는 장소명입니다.");
        }
        Location location = Location.builder().name(dto.getName()).build();
        return locationRepository.save(location).getId();
    }

    @Transactional
    public void updateLocation(Long id, LocationRequest dto) {
        log.info("[UPDATE] Location 수정 요청 - ID: {}", id);
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("장소를 찾을 수 없습니다."));
        location.updateLocation(dto.getName());
    }

    @Transactional
    public void deleteLocation(Long id) {
        log.info("[DELETE] Location 삭제 요청 - ID: {}", id);
        if (rackRepository.existsByLocationId(id)) {
            log.error("삭제 실패 - 해당 장소에 등록된 Rack이 존재함. ID: {}", id);
            throw new ResourceInUseException("등록된 랙이 있어 삭제할 수 없습니다.");
        }
        locationRepository.deleteById(id);
    }

    public List<LocationResponse> getAll() {
        return locationRepository.findAll().stream().map(LocationResponse::new).toList();
    }
}
