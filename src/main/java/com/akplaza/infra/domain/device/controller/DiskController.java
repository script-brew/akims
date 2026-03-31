package com.akplaza.infra.domain.device.controller;

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

import com.akplaza.infra.domain.device.dto.DiskCreateRequest;
import com.akplaza.infra.domain.device.dto.DiskResponse;
import com.akplaza.infra.domain.device.dto.DiskUpdateRequest;
import com.akplaza.infra.domain.device.service.DiskService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "8. Disk API", description = "서버에 장착된 디스크(스토리지) 용량 및 마운트 포인트 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/disks")
@RequiredArgsConstructor
public class DiskController {

    private final DiskService diskService;

    @Operation(summary = "디스크 추가", description = "특정 서버에 새로운 디스크를 장착(추가)합니다.")
    @PostMapping
    public ResponseEntity<Long> createDisk(@Valid @RequestBody DiskCreateRequest request) {
        log.info("Disk API: 등록 요청 - Target Server ID: {}", request.getServerId());
        Long diskId = diskService.createDisk(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(diskId);
    }

    @Operation(summary = "디스크 단건 조회", description = "특정 디스크의 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<DiskResponse> getDisk(@PathVariable Long id) {
        log.debug("Disk API: 단건 조회 요청 - ID: {}", id);
        DiskResponse response = diskService.getDisk(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "특정 서버의 디스크 목록 조회", description = "서버 상세 화면에서 해당 서버에 장착된 모든 디스크 목록을 조회합니다.")
    @GetMapping("/server/{serverId}")
    public ResponseEntity<List<DiskResponse>> getDisksByServerId(@PathVariable Long serverId) {
        log.debug("Disk API: 서버 종속 디스크 조회 요청 - Server ID: {}", serverId);
        List<DiskResponse> responses = diskService.getDisksByServerId(serverId);
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "디스크 정보 수정", description = "특정 디스크의 용량이나 마운트 포인트를 변경합니다.")
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateDisk(
            @PathVariable Long id,
            @Valid @RequestBody DiskUpdateRequest request) {
        log.info("Disk API: 수정 요청 - ID: {}", id);
        diskService.updateDisk(id, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "디스크 삭제(제거)", description = "서버에서 특정 디스크를 제거합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDisk(@PathVariable Long id) {
        log.info("Disk API: 삭제 요청 - ID: {}", id);
        diskService.deleteDisk(id);
        return ResponseEntity.noContent().build();
    }
}