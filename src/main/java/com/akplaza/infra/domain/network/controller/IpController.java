package com.akplaza.infra.domain.network.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.akplaza.infra.domain.network.dto.IpCreateRequest;
import com.akplaza.infra.domain.network.dto.IpResponse;
import com.akplaza.infra.domain.network.entity.AssignedType;
import com.akplaza.infra.domain.network.service.IpService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "4. IP 자산 API", description = "개별 IP 자산 등록 및 현황 조회 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/ips")
@RequiredArgsConstructor
public class IpController {

    private final IpService ipService;

    @Operation(summary = "IP 자산 풀(Pool) 등록", description = "특정 IP 대역에 속하는 개별 IP 자산을 시스템에 등록합니다. (기본 미할당 상태로 생성됨)")
    @PostMapping
    public ResponseEntity<Long> registerIp(@Valid @RequestBody IpCreateRequest request) {
        log.info("Ip API: 등록 요청 - IP: {}", request.getIpAddress());
        Long ipId = ipService.registerIp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ipId);
    }

    @Operation(summary = "전체 IP 자산 조회", description = "시스템에 등록된 전체 IP 자산과 그 할당 상태(사용 유무, 점유 장비)를 조회합니다.")
    @GetMapping
    public ResponseEntity<List<IpResponse>> getAllIps() {
        log.debug("Ip API: 전체 목록 조회 요청");
        List<IpResponse> responses = ipService.getAllIps();
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "특정 장비의 IP 목록 조회", description = "서버 또는 네트워크 장비에 할당된 IP 목록을 조회합니다.")
    @GetMapping("/target")
    public ResponseEntity<List<IpResponse>> getIpsByTarget(
            @Parameter(description = "대상 타입 (SERVER, NETWORK_DEVICE, VIP 등)", required = true) @RequestParam AssignedType type,

            @Parameter(description = "대상의 ID (서버 ID 또는 네트워크 장비 ID)", required = true) @RequestParam Long targetId) {

        log.debug("Ip API: 타겟 기반 IP 조회 요청 - Type: {}, Target ID: {}", type, targetId);
        List<IpResponse> responses = ipService.getIpsByTarget(type, targetId);
        return ResponseEntity.ok(responses);
    }
}