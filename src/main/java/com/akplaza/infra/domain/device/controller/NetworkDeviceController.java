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

import com.akplaza.infra.domain.device.dto.NetworkDeviceCreateRequest;
import com.akplaza.infra.domain.device.dto.NetworkDeviceResponse;
import com.akplaza.infra.domain.device.dto.NetworkDeviceUpdateRequest;
import com.akplaza.infra.domain.device.service.NetworkDeviceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "7. Network Device API", description = "네트워크 장비(L2, L3, L4, 방화벽 등) 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/network-devices")
@RequiredArgsConstructor
public class NetworkDeviceController {

    private final NetworkDeviceService networkDeviceService;

    @Operation(summary = "네트워크 장비 등록", description = "새로운 네트워크 장비를 등록합니다. IP(VIP 포함) 할당 로직이 연계됩니다.")
    @PostMapping
    public ResponseEntity<Long> createNetworkDevice(@Valid @RequestBody NetworkDeviceCreateRequest request) {
        log.info("NetworkDevice API: 등록 요청 - Name: {}", request.getName());
        Long deviceId = networkDeviceService.createNetworkDevice(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(deviceId);
    }

    @Operation(summary = "네트워크 장비 단건 조회", description = "특정 네트워크 장비의 상세 정보 및 매핑된 IP 목록을 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<NetworkDeviceResponse> getNetworkDevice(@PathVariable Long id) {
        log.debug("NetworkDevice API: 단건 조회 요청 - ID: {}", id);
        NetworkDeviceResponse response = networkDeviceService.getNetworkDevice(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "네트워크 장비 전체 조회", description = "시스템에 등록된 전체 네트워크 장비 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<List<NetworkDeviceResponse>> getAllNetworkDevices() {
        log.debug("NetworkDevice API: 전체 목록 조회 요청");
        List<NetworkDeviceResponse> responses = networkDeviceService.getAllNetworkDevices();
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "네트워크 장비 수정", description = "특정 네트워크 장비의 속성을 수정합니다.")
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateNetworkDevice(
            @PathVariable Long id,
            @Valid @RequestBody NetworkDeviceUpdateRequest request) {
        log.info("NetworkDevice API: 수정 요청 - ID: {}", id);
        networkDeviceService.updateNetworkDevice(id, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "네트워크 장비 삭제", description = "특정 네트워크 장비를 삭제하며, 점유 중이던 IP는 자동 회수됩니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNetworkDevice(@PathVariable Long id) {
        log.info("NetworkDevice API: 삭제 요청 - ID: {}", id);
        networkDeviceService.deleteNetworkDevice(id);
        return ResponseEntity.noContent().build();
    }
}