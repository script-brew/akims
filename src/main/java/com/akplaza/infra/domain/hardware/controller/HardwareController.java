package com.akplaza.infra.domain.hardware.controller;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.akplaza.infra.domain.hardware.dto.HardwareCreateRequest;
import com.akplaza.infra.domain.hardware.dto.HardwareResponse;
import com.akplaza.infra.domain.hardware.dto.HardwareUpdateRequest;
import com.akplaza.infra.domain.hardware.service.HardwareService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "5. Hardware API", description = "데이터센터 물리 하드웨어(서버, 스위치, 스토리지 장비 등) 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/hardwares")
@RequiredArgsConstructor
public class HardwareController {

    private final HardwareService hardwareService;

    @Operation(summary = "하드웨어 장비 등록", description = "새로운 물리 하드웨어 장비를 시스템에 등록합니다. (랙 실장 정보 포함 가능)")
    @PostMapping
    public ResponseEntity<Long> createHardware(@Valid @RequestBody HardwareCreateRequest request) {
        log.info("Hardware API: 등록 요청 - SerialNo: {}, Model: {}", request.getSerialNo(), request.getModel());
        Long hardwareId = hardwareService.createHardware(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(hardwareId);
    }

    @Operation(summary = "하드웨어 단건 조회", description = "특정 하드웨어 장비의 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<HardwareResponse> getHardware(@PathVariable Long id) {
        log.debug("Hardware API: 단건 조회 요청 - ID: {}", id);
        HardwareResponse response = hardwareService.getHardware(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<HardwareResponse>> getHardwares(
            @RequestParam Map<String, String> searchParams, // 🌟 핵심: 프론트가 보내는 모든 필터를 Map으로 흡수
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("id").descending());

        return ResponseEntity.ok(hardwareService.searchHardwares(searchParams, pageRequest));
    }

    // @Operation(summary = "하드웨어 전체 조회", description = "시스템에 등록된 전체 물리 하드웨어 목록을
    // 조회합니다.")
    // @GetMapping
    // public ResponseEntity<List<HardwareResponse>> getAllHardwares() {
    // log.debug("Hardware API: 전체 목록 조회 요청");
    // List<HardwareResponse> responses = hardwareService.getAllHardwares();
    // return ResponseEntity.ok(responses);
    // }

    @Operation(summary = "하드웨어 정보 수정", description = "특정 하드웨어의 정보(랙 위치 변경, 설명 수정 등)를 업데이트합니다.")
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateHardware(
            @PathVariable Long id,
            @Valid @RequestBody HardwareUpdateRequest request) {
        log.info("Hardware API: 수정 요청 - ID: {}", id);
        hardwareService.updateHardware(id, request);
        return ResponseEntity.ok().build(); // 200 OK
    }

    @Operation(summary = "하드웨어 삭제(폐기)", description = "특정 물리 하드웨어를 삭제(폐기) 처리합니다. 단, 해당 장비 위에 구동 중인 논리적 서버나 네트워크 장비가 있을 경우 삭제할 수 없습니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHardware(@PathVariable Long id) {
        log.info("Hardware API: 삭제 요청 - ID: {}", id);
        hardwareService.deleteHardware(id);
        return ResponseEntity.noContent().build(); // 204 No Content
    }
}