package com.akplaza.infra.domain.network.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.akplaza.infra.domain.network.dto.IpCidrCreateRequest;
import com.akplaza.infra.domain.network.service.IpService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "3. IP CIDR API", description = "네트워크 IP 대역(Subnet) 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/ip-cidrs")
@RequiredArgsConstructor
public class IpCidrController {

    private final IpService ipService;

    @Operation(summary = "IP 대역 등록", description = "새로운 IP 대역폭(예: 192.168.1.0/24) 마스터 데이터를 등록합니다.")
    @PostMapping
    public ResponseEntity<Long> createIpCidr(@Valid @RequestBody IpCidrCreateRequest request) {
        log.info("IpCidr API: 등록 요청 - Block: {}", request.getCidrBlock());
        Long ipCidrId = ipService.createIpCidr(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ipCidrId);
    }

    // 💡 실무 Tip: 필요에 따라 IP 대역 전체 조회(getAllIpCidrs), 삭제(deleteIpCidr) API를
    // IpService에 추가한 뒤 이곳에 연동하여 확장할 수 있습니다.
}