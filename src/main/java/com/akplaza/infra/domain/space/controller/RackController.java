package com.akplaza.infra.domain.space.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.akplaza.infra.domain.space.dto.RackRequest;
import com.akplaza.infra.domain.space.dto.RackResponse;
import com.akplaza.infra.domain.space.service.RackService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "2. Rack API", description = "데이터센터 내 랙(Rack) 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/racks")
@RequiredArgsConstructor
public class RackController {

    private final RackService rackService;

    @Operation(summary = "랙 등록", description = "특정 장소(Location)에 새로운 랙을 등록합니다.")
    @PostMapping
    public ResponseEntity<Long> createRack(@Valid @RequestBody RackRequest request) {
        log.info("Rack API: 등록 요청 - RackNo: {}", request.getRackNo());
        Long rackId = rackService.createRack(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(rackId);
    }

    @Operation(summary = "랙 전체 조회", description = "시스템에 등록된 모든 랙 목록을 장소 정보와 함께 조회합니다.")
    @GetMapping
    public ResponseEntity<List<RackResponse>> getAllRacks() {
        log.debug("Rack API: 전체 목록 조회 요청");
        List<RackResponse> responses = rackService.getAll();
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "랙 삭제", description = "특정 랙을 삭제합니다. 단, 랙에 실장된 하드웨어가 있으면 삭제할 수 없습니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRack(@PathVariable Long id) {
        log.info("Rack API: 삭제 요청 - ID: {}", id);
        rackService.deleteRack(id);
        return ResponseEntity.noContent().build(); // 204 No Content
    }
}