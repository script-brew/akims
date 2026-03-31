package com.akplaza.infra.domain.network.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.akplaza.infra.domain.network.dto.IpCidrCreateRequest;
import com.akplaza.infra.domain.network.dto.IpCreateRequest;
import com.akplaza.infra.domain.network.dto.IpResponse;
import com.akplaza.infra.domain.network.entity.AssignedType;
import com.akplaza.infra.domain.network.entity.Ip;
import com.akplaza.infra.domain.network.entity.IpCidr;
import com.akplaza.infra.domain.network.repository.IpCidrRepository;
import com.akplaza.infra.domain.network.repository.IpRepository;
import com.akplaza.infra.global.error.exception.DuplicateResourceException;
import com.akplaza.infra.global.error.exception.ResourceInUseException;
import com.akplaza.infra.global.error.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IpService {

    private final IpRepository ipRepository;
    private final IpCidrRepository ipCidrRepository;

    // ==========================================
    // 1. 타 도메인(Server, NetworkDevice)에서 호출하는 핵심 연동 로직
    // ==========================================

    /**
     * 특정 장비에 IP를 할당합니다. (ServerService 등에서 트랜잭션 내에 호출됨)
     */
    @Transactional
    public void allocateIp(String ipAddress, AssignedType targetType, Long targetId) {
        log.debug("IP 할당 프로세스 진입 - IP: {}, 대상 타입: {}, 대상 ID: {}", ipAddress, targetType, targetId);

        // [예외 포인트 1] 등록된 IP인지 확인
        Ip ip = ipRepository.findByIpAddress(ipAddress)
                .orElseThrow(() -> {
                    log.error("IP 할당 실패 - 시스템에 미등록된 IP 주소: {}", ipAddress);
                    return new ResourceNotFoundException("등록되지 않은 IP 주소입니다. 먼저 IP 자산을 등록해주세요: " + ipAddress);
                });

        // [예외 포인트 2] 이미 사용 중인지 확인 (동시성 방지)
        if (ip.isUsed()) {
            log.error("IP 할당 실패 - 이미 점유된 IP: {} (현재 소유: {})", ipAddress, ip.getAssignedType());
            throw new ResourceInUseException("이미 다른 장비에 할당되어 사용 중인 IP입니다: " + ipAddress);
        }

        // 엔티티 내부 메서드를 통한 상태 변경 (객체지향 설계)
        ip.allocate(targetType, targetId);
        log.info("IP 할당 성공 - IP: {} -> Target [Type: {}, ID: {}]", ipAddress, targetType, targetId);
    }

    /**
     * 특정 장비가 삭제되거나 수정될 때, 점유 중이던 IP를 회수(초기화)합니다.
     */
    @Transactional
    public void releaseIpForTarget(AssignedType targetType, Long targetId) {
        log.debug("IP 일괄 회수 프로세스 진입 - 대상 타입: {}, 대상 ID: {}", targetType, targetId);

        List<Ip> targetIps = ipRepository.findByAssignedTypeAndAssignedId(targetType, targetId);

        if (targetIps.isEmpty()) {
            log.debug("회수할 IP가 없습니다. Target [Type: {}, ID: {}]", targetType, targetId);
            return;
        }

        for (Ip ip : targetIps) {
            ip.release();
            log.info("IP 회수 성공 - 반환된 IP: {}", ip.getIpAddress());
        }
    }

    // ==========================================
    // 2. 자체 CRUD 로직 (IP 대역 마스터 및 개별 IP 관리)
    // ==========================================

    @Transactional
    public Long createIpCidr(IpCidrCreateRequest dto) {
        log.info("IP 대역폭(CIDR) 등록 요청 - Block: {}", dto.getCidrBlock());

        if (ipCidrRepository.existsByCidrBlock(dto.getCidrBlock())) {
            log.error("CIDR 등록 실패 - 중복된 대역: {}", dto.getCidrBlock());
            throw new DuplicateResourceException("이미 존재하는 네트워크 대역입니다.");
        }

        IpCidr ipCidr = IpCidr.builder()
                .cidrBlock(dto.getCidrBlock())
                .description(dto.getDescription())
                .build();

        ipCidrRepository.save(ipCidr);
        log.info("IP 대역폭 등록 완료 - ID: {}", ipCidr.getId());
        return ipCidr.getId();
    }

    @Transactional
    public Long registerIp(IpCreateRequest dto) {
        log.info("신규 IP 자산 등록 요청 - IP: {}", dto.getIpAddress());

        // [예외 포인트 3] IP 중복 등록 방지
        if (ipRepository.existsByIpAddress(dto.getIpAddress())) {
            log.error("IP 등록 실패 - 이미 시스템에 존재하는 IP: {}", dto.getIpAddress());
            throw new DuplicateResourceException("이미 자산으로 등록된 IP 주소입니다.");
        }

        IpCidr ipCidr = ipCidrRepository.findById(dto.getIpCidrId())
                .orElseThrow(() -> new ResourceNotFoundException("지정된 IP 대역(CIDR)을 찾을 수 없습니다."));

        // 기본적으로 미할당 상태로 생성됨
        Ip ip = Ip.builder()
                .ipAddress(dto.getIpAddress())
                .isUsed(false)
                .assignedType(AssignedType.NONE)
                .build();

        // 양방향 연관관계 편의 메서드 호출
        ipCidr.addIp(ip);
        ipRepository.save(ip);

        log.info("신규 IP 자산 등록 완료 - ID: {}", ip.getId());
        return ip.getId();
    }

    // 전체 IP 목록 조회 (화면의 IP 자산 관리용)
    public List<IpResponse> getAllIps() {
        log.debug("전체 IP 자산 목록 조회 요청");
        return ipRepository.findAll().stream()
                .map(IpResponse::new)
                .collect(Collectors.toList());
    }

    // 특정 서버나 장비에 할당된 IP만 조회할 경우
    public List<IpResponse> getIpsByTarget(AssignedType type, Long targetId) {
        return ipRepository.findByAssignedTypeAndAssignedId(type, targetId).stream()
                .map(IpResponse::new)
                .collect(Collectors.toList());
    }
}