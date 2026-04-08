package com.akplaza.infra.domain.network.controller;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.akplaza.infra.domain.network.dto.IpCidrCreateRequest;
import com.akplaza.infra.domain.network.dto.IpCidrDetailResponse;
import com.akplaza.infra.domain.network.dto.IpCidrResponse;
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

    @GetMapping
    public ResponseEntity<Page<IpCidrResponse>> getIpCidrs(
            @RequestParam Map<String, String> searchParams, // 🌟 핵심: 프론트가 보내는 모든 필터를 Map으로 흡수
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("id").descending());

        return ResponseEntity.ok(ipService.searchIpCidrs(searchParams, pageRequest));
    }

    // // 🌟 추가: 전체 대역 목록 조회
    // @Operation(summary = "전체 IP 대역 조회", description = "시스템에 등록된 모든 IP 대역 목록을
    // 조회합니다.")
    // @GetMapping
    // public ResponseEntity<List<IpCidrResponse>> getAllIpCidrs() {
    // return ResponseEntity.ok(ipService.getAllIpCidrs());
    // }

    // 🌟 15년 차의 핵심: Visual IP Map을 그리기 위한 상세 조회
    @Operation(summary = "특정 IP 대역 및 소속 IP 현황 조회", description = "Visual Map 렌더링을 위해 특정 대역에 속한 모든 개별 IP의 현황을 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<IpCidrDetailResponse> getIpCidrDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ipService.getIpCidrDetail(id));
    }

    // 🌟 추가: IP 대역 삭제
    @Operation(summary = "IP 대역 삭제", description = "특정 IP 대역을 삭제합니다. 사용 중인 IP가 있으면 거부됩니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIpCidr(@PathVariable Long id) {
        ipService.deleteIpCidr(id);
        return ResponseEntity.noContent().build();
    }
}