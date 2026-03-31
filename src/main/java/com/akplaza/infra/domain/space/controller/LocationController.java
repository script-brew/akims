package com.akplaza.infra.domain.space.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.akplaza.infra.domain.space.dto.LocationRequest;
import com.akplaza.infra.domain.space.dto.LocationResponse;
import com.akplaza.infra.domain.space.service.LocationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "1. Location API", description = "데이터센터 및 물리적 장소 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @Operation(summary = "장소 등록", description = "새로운 물리적 장소(예: 상암 데이터센터, 분당 IDC)를 등록합니다.")
    @PostMapping
    public ResponseEntity<Long> createLocation(@Valid @RequestBody LocationRequest request) {
        log.info("Location API: 등록 요청 - {}", request.getName());
        Long locationId = locationService.createLocation(request);
        // 생성 성공 시 201 Created 상태 코드와 함께 생성된 ID 반환
        return ResponseEntity.status(HttpStatus.CREATED).body(locationId);
    }

    @Operation(summary = "장소 전체 조회", description = "등록된 모든 장소 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<List<LocationResponse>> getAllLocations() {
        log.debug("Location API: 전체 목록 조회 요청");
        List<LocationResponse> responses = locationService.getAll();
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "장소 수정", description = "특정 장소의 이름을 수정합니다.")
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateLocation(
            @PathVariable Long id,
            @Valid @RequestBody LocationRequest request) {
        log.info("Location API: 수정 요청 - ID: {}", id);
        locationService.updateLocation(id, request);
        return ResponseEntity.ok().build(); // 200 OK
    }

    @Operation(summary = "장소 삭제", description = "특정 장소를 삭제합니다. 단, 해당 장소에 등록된 랙(Rack)이 있으면 삭제할 수 없습니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLocation(@PathVariable Long id) {
        log.info("Location API: 삭제 요청 - ID: {}", id);
        locationService.deleteLocation(id);
        // 삭제 완료 시 204 No Content 상태 코드 반환 (본문 없음)
        return ResponseEntity.noContent().build();
    }
}