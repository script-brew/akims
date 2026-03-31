package com.akplaza.infra.domain.software.controller;

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

import com.akplaza.infra.domain.software.dto.SoftwareCreateRequest;
import com.akplaza.infra.domain.software.dto.SoftwareResponse;
import com.akplaza.infra.domain.software.dto.SoftwareUpdateRequest;
import com.akplaza.infra.domain.software.service.SoftwareService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "9. Software API", description = "서버에 설치된 소프트웨어(미들웨어, DBMS 등) 자산 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/softwares")
@RequiredArgsConstructor
public class SoftwareController {

    private final SoftwareService softwareService;

    @Operation(summary = "소프트웨어 등록", description = "특정 서버에 설치된 소프트웨어 정보를 등록합니다.")
    @PostMapping
    public ResponseEntity<Long> createSoftware(@Valid @RequestBody SoftwareCreateRequest request) {
        log.info("Software API: 등록 요청 - Target Server ID: {}, Name: {}", request.getServerId(), request.getName());
        Long softwareId = softwareService.createSoftware(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(softwareId);
    }

    @Operation(summary = "소프트웨어 단건 조회", description = "특정 소프트웨어의 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<SoftwareResponse> getSoftware(@PathVariable Long id) {
        log.debug("Software API: 단건 조회 요청 - ID: {}", id);
        SoftwareResponse response = softwareService.getSoftware(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "특정 서버의 소프트웨어 목록 조회", description = "서버 상세 화면에서 해당 서버에 설치된 모든 소프트웨어 목록을 조회합니다.")
    @GetMapping("/server/{serverId}")
    public ResponseEntity<List<SoftwareResponse>> getSoftwareByServerId(@PathVariable Long serverId) {
        log.debug("Software API: 서버 종속 소프트웨어 조회 요청 - Server ID: {}", serverId);
        List<SoftwareResponse> responses = softwareService.getSoftwareByServerId(serverId);
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "소프트웨어 정보 수정", description = "설치된 소프트웨어의 버전이나 용도, 유지보수 정보를 수정합니다.")
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateSoftware(
            @PathVariable Long id,
            @Valid @RequestBody SoftwareUpdateRequest request) {
        log.info("Software API: 수정 요청 - ID: {}", id);
        softwareService.updateSoftware(id, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "소프트웨어 삭제", description = "서버에서 특정 소프트웨어를 제거(삭제)합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSoftware(@PathVariable Long id) {
        log.info("Software API: 삭제 요청 - ID: {}", id);
        softwareService.deleteSoftware(id);
        return ResponseEntity.noContent().build();
    }
}