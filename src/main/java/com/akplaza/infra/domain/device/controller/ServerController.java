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

import com.akplaza.infra.domain.device.dto.ServerCreateRequest;
import com.akplaza.infra.domain.device.dto.ServerResponse;
import com.akplaza.infra.domain.device.dto.ServerUpdateRequest;
import com.akplaza.infra.domain.device.service.ServerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "6. Server API", description = "서버(물리/가상/클라우드) 인스턴스 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/servers")
@RequiredArgsConstructor
public class ServerController {

    private final ServerService serverService;

    @Operation(summary = "서버 등록", description = "새로운 서버 인스턴스를 등록합니다. IP 목록을 함께 전송하면 자동 할당됩니다.")
    @PostMapping
    public ResponseEntity<Long> createServer(@Valid @RequestBody ServerCreateRequest request) {
        log.info("Server API: 등록 요청 - HostName: {}", request.getName());
        Long serverId = serverService.createServer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(serverId); // 201 Created
    }

    @Operation(summary = "서버 단건 조회", description = "특정 서버의 상세 정보(하드웨어, IP 정보 포함)를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<ServerResponse> getServer(@PathVariable Long id) {
        log.debug("Server API: 단건 조회 요청 - ID: {}", id);
        ServerResponse response = serverService.getServer(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "서버 전체 조회", description = "시스템에 등록된 전체 서버 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<List<ServerResponse>> getAllServers() {
        log.debug("Server API: 전체 목록 조회 요청");
        List<ServerResponse> responses = serverService.getAllServers();
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "서버 정보 수정", description = "특정 서버의 정보(스펙 업그레이드, OS 변경 등)를 업데이트합니다.")
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateServer(
            @PathVariable Long id,
            @Valid @RequestBody ServerUpdateRequest request) {
        log.info("Server API: 수정 요청 - ID: {}", id);
        serverService.updateServer(id, request);
        return ResponseEntity.ok().build(); // 200 OK
    }

    @Operation(summary = "서버 삭제", description = "특정 서버를 삭제합니다. 삭제 시 할당되었던 IP는 자동으로 회수(초기화)됩니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteServer(@PathVariable Long id) {
        log.info("Server API: 삭제 요청 - ID: {}", id);
        serverService.deleteServer(id);
        return ResponseEntity.noContent().build(); // 204 No Content
    }
}