package com.akplaza.infra.domain.device.service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.akplaza.infra.domain.device.dto.ServerCreateRequest;
import com.akplaza.infra.domain.device.dto.ServerResponse;
import com.akplaza.infra.domain.device.dto.ServerUpdateRequest;
import com.akplaza.infra.domain.device.entity.Server;
import com.akplaza.infra.domain.device.entity.Disk;
import com.akplaza.infra.domain.device.entity.ServerSpec;
import com.akplaza.infra.domain.device.entity.ServerType;
import com.akplaza.infra.domain.device.repository.ServerRepository;
import com.akplaza.infra.domain.device.repository.DiskRepository;
import com.akplaza.infra.domain.hardware.dto.HardwareResponse;
import com.akplaza.infra.domain.hardware.entity.Hardware;
import com.akplaza.infra.domain.hardware.repository.HardwareRepository;
import com.akplaza.infra.domain.network.dto.IpAssignRequest;
import com.akplaza.infra.domain.network.entity.AssignedType;
import com.akplaza.infra.domain.network.entity.Ip;
import com.akplaza.infra.domain.network.repository.IpRepository;
import com.akplaza.infra.domain.network.service.IpService;
import com.akplaza.infra.domain.software.entity.Software;
import com.akplaza.infra.domain.software.repository.SoftwareRepository;
import com.akplaza.infra.domain.software.service.SoftwareService;
import com.akplaza.infra.global.error.exception.DuplicateResourceException;
import com.akplaza.infra.global.error.exception.ResourceNotFoundException;
import com.akplaza.infra.global.common.repository.DynamicSearchSpec;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 전체 읽기 전용으로 설정하여 JPA 1차 캐시 스냅샷 생성 방지 (성능 최적화)
public class ServerService {

    private final ServerRepository serverRepository;
    private final HardwareRepository hardwareRepository;
    private final IpService ipService; // IP 할당/해제 비즈니스 로직 호출용
    private final IpRepository ipRepository; // 조회용
    private final DiskService diskService; // 디스크 할당용
    private final DiskRepository diskRepository; // 조회용
    private final SoftwareRepository softwareRepository; // 조회용
    private final SoftwareService softwareService; // 소프트웨어 할당용

    // ==========================================
    // 1. 서버 생성 (CREATE)
    // ==========================================
    @Transactional // 쓰기 작업이므로 트랜잭션 활성화 (예외 발생 시 자동 Rollback)
    public Long createServer(ServerCreateRequest dto) {
        log.info("서버 등록 트랜잭션 시작 - HostName: {}", dto.getHostName());

        // [예외 포인트 1] HostName 중복 검증
        if (serverRepository.existsByHostName(dto.getHostName())) {
            log.error("서버 등록 실패 - 중복된 HostName: {}", dto.getHostName());
            throw new DuplicateResourceException("이미 존재하는 서버명입니다: " + dto.getHostName());
        }

        // [예외 포인트 2] 하드웨어 종속성 검증
        Hardware hardware = validateAndGetHardware(dto.getHardwareId(), dto.getServerType());

        // 엔티티 생성
        Server server = Server.builder()
                .hardware(hardware)
                .hostName(dto.getHostName())
                .serverCategory(dto.getServerCategory())
                .environment(dto.getEnvironment())
                .serverType(dto.getServerType())
                .platform(dto.getPlatform())
                .os(dto.getOs())
                .description(dto.getDescription())
                .spec(new ServerSpec(dto.getCpuCore(), dto.getMemoryGb()))
                .ha(dto.isHa())
                .backupInfo(dto.getBackupInfo())
                .monitoringInfo(dto.getMonitoringInfo())
                .build();

        Server savedServer = serverRepository.save(server);
        log.debug("서버 기본 정보 저장 완료 - ID: {}", savedServer.getId());

        // 🌟 N개의 IP 할당 루프
        if (dto.getIps() != null && !dto.getIps().isEmpty()) {
            for (IpAssignRequest ipReq : dto.getIps()) {
                ipService.allocateIp(ipReq.getIpCidrId(), ipReq.getIpAddress(), AssignedType.SERVER,
                        savedServer.getId());
            }
        }

        diskService.syncDisksToServer(savedServer.getId(), dto.getDisks());
        softwareService.syncSoftwaresToServer(savedServer.getId(), dto.getSoftwares());

        log.info("서버 등록 트랜잭션 성공적으로 완료 - 최종 ID: {}", savedServer.getId());
        return savedServer.getId();
    }

    // ==========================================
    // 2. 서버 조회 (READ)
    // ==========================================
    public ServerResponse getServer(Long id) {
        log.debug("서버 단건 조회 요청 - ID: {}", id);

        Server server = serverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("서버를 찾을 수 없습니다. ID: " + id));

        // 해당 서버에 할당된 IP 목록 조회
        List<Ip> assignedIps = ipRepository.findByAssignedTypeAndAssignedId(AssignedType.SERVER, server.getId());
        List<Disk> disks = diskRepository.findByServerId(server.getId());
        List<Software> softwares = softwareRepository.findByServerId(server.getId());

        return new ServerResponse(server, assignedIps, disks, softwares);
    }

    @Transactional(readOnly = true)
    public Page<ServerResponse> searchServers(Map<String, String> searchParams, Pageable pageable) {

        // 1. 공통 유틸을 통해 동적 WHERE 절(Specification) 생성
        Specification<Server> spec = DynamicSearchSpec.searchConditions(searchParams);

        // 2. Repository의 findAll(spec, pageable) 호출하여 기본 서버 목록 조회
        Page<Server> serverPage = serverRepository.findAll(spec, pageable);
        List<Server> servers = serverPage.getContent();

        // 결과가 없으면 빈 페이지 반환
        if (servers.isEmpty()) {
            return new PageImpl<>(java.util.Collections.emptyList(), pageable, serverPage.getTotalElements());
        }

        // 3. 🚨 N+1 방어 로직: 조회된 서버 ID들만 추출
        List<Long> serverIds = servers.stream().map(Server::getId).collect(Collectors.toList());

        // 4. IP 목록 한 번에 조회 후 서버 ID를 키값으로 그룹핑 (Map 생성)
        List<Ip> allIps = ipRepository.findByAssignedTypeAndAssignedIdIn(AssignedType.SERVER, serverIds);
        Map<Long, List<Ip>> ipMap = allIps.stream().collect(Collectors.groupingBy(Ip::getAssignedId));

        // 5. 🌟 DTO로 안전하게 조립 (데이터 중복 증식 방지)
        List<ServerResponse> responseList = servers.stream().map(server -> {
            List<Ip> mappedIps = ipMap.getOrDefault(server.getId(), java.util.Collections.emptyList());

            // ServerResponse 생성자에 엔티티의 연관 객체들을 넘겨주면, DTO 클래스 안에서 필요한 것만 예쁘게 파싱함
            return new ServerResponse(server, mappedIps, server.getDisks(), server.getSoftwares());
        }).collect(Collectors.toList());

        // 6. 최종 Page 객체 반환
        return new PageImpl<>(responseList, pageable, serverPage.getTotalElements());
    }

    public List<ServerResponse> getAllServers() {
        log.debug("전체 서버 목록 조회 요청");
        // FETCH JOIN이 적용된 Repository 메서드를 사용하여 N+1 쿼리 문제 방지
        List<Server> servers = serverRepository.findAllWithHardwareAndRack();
        if (servers.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        List<Long> serverIds = servers.stream().map(Server::getId).collect(Collectors.toList());
        List<Ip> allIps = ipRepository.findByAssignedTypeAndAssignedIdIn(AssignedType.SERVER, serverIds);
        List<Disk> allDisks = diskRepository.findByServerIdIn(serverIds);
        List<Software> allSoftwares = softwareRepository.findByServerIdIn(serverIds);

        // 🌟 장비 ID를 Key로 하여 소속된 IP 목록(List)을 그룹핑합니다.
        java.util.Map<Long, List<Ip>> ipMap = allIps.stream()
                .collect(Collectors.groupingBy(Ip::getAssignedId));

        java.util.Map<Server, List<Disk>> diskMap = allDisks.stream()
                .collect(Collectors.groupingBy(Disk::getServer));

        java.util.Map<Server, List<Software>> softwareMap = allSoftwares.stream()
                .collect(Collectors.groupingBy(Software::getServer));

        return servers.stream().map(server -> {
            // 해당 서버에 할당된 IP 리스트를 가져와서 DTO에 던짐
            List<Ip> mappedIps = ipMap.getOrDefault(server.getId(), java.util.Collections.emptyList());
            List<Disk> mappedDisks = diskMap.getOrDefault(server, java.util.Collections.emptyList());
            List<Software> softwares = softwareMap.getOrDefault(server, java.util.Collections.emptyList());

            return new ServerResponse(server, mappedIps, mappedDisks, softwares);
        }).collect(Collectors.toList());
    }

    // ==========================================
    // 3. 서버 수정 (UPDATE)
    // ==========================================
    @Transactional
    public void updateServer(Long id, ServerUpdateRequest dto) {
        log.info("서버 수정 트랜잭션 시작 - ID: {}", id);

        Server server = serverRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("서버 수정 실패 - 존재하지 않는 서버 ID: {}", id);
                    return new ResourceNotFoundException("서버를 찾을 수 없습니다. ID: " + id);
                });

        // [예외 포인트 3] 이름이 변경된 경우 중복 체크
        if (!server.getHostName().equals(dto.getHostName()) && serverRepository.existsByHostName(dto.getHostName())) {
            log.error("서버 수정 실패 - 변경하려는 HostName이 이미 존재함: {}", dto.getHostName());
            throw new DuplicateResourceException("이미 존재하는 서버명입니다: " + dto.getHostName());
        }

        // [예외 포인트 4] 하드웨어 변경 처리
        Hardware newHardware = validateAndGetHardware(dto.getHardwareId(), server.getServerType());

        // 🌟 추가된 부분: IP 정보가 변경되었을 수 있으므로, 기존 IP 해제 후 새 IP 재할당
        ipService.releaseIpForTarget(AssignedType.SERVER, server.getId());

        // 🌟 N개의 IP 할당 루프
        if (dto.getIps() != null && !dto.getIps().isEmpty()) {
            for (IpAssignRequest ipReq : dto.getIps()) {
                ipService.allocateIp(ipReq.getIpCidrId(), ipReq.getIpAddress(), AssignedType.SERVER,
                        server.getId());
            }
        }
        diskService.syncDisksToServer(server.getId(), dto.getDisks());
        softwareService.syncSoftwaresToServer(server.getId(), dto.getSoftwares());

        server.updateServerInfo(
                dto.getHostName(),
                dto.getServerCategory(),
                dto.getEnvironment(),
                dto.getOs(),
                dto.getDescription(),
                new ServerSpec(dto.getCpuCore(), dto.getMemoryGb()),
                dto.isHa(),
                dto.getBackupInfo(),
                dto.getMonitoringInfo(),
                newHardware);

        log.info("서버 수정 트랜잭션 완료 - ID: {}", id);
    }

    // ==========================================
    // 4. 서버 삭제 (DELETE)
    // ==========================================
    @Transactional
    public void deleteServer(Long id) {
        log.info("서버 삭제 트랜잭션 시작 - ID: {}", id);

        Server server = serverRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("서버 삭제 실패 - 존재하지 않는 서버 ID: {}", id);
                    return new ResourceNotFoundException("삭제할 서버를 찾을 수 없습니다. ID: " + id);
                });

        // [비즈니스 로직 연계] 서버를 삭제하기 전, 점유하고 있던 IP 반환(Release) 처리
        log.debug("서버 삭제 전, 할당된 IP 반환 처리 실행");
        ipService.releaseIpForTarget(AssignedType.SERVER, server.getId());

        // 하위의 Disk, Software 등은 CascadeType.ALL + orphanRemoval = true 설정에 의해 연쇄 삭제됨
        serverRepository.delete(server);

        log.info("서버 삭제 트랜잭션 완료 - ID: {}", id);
    }

    // ==========================================
    // 내부 헬퍼 메서드
    // ==========================================

    // 물리 서버일 경우 하드웨어 ID 필수 검증 및 엔티티 반환
    private Hardware validateAndGetHardware(Long hardwareId, ServerType serverType) {
        if (hardwareId != null) {
            return hardwareRepository.findById(hardwareId)
                    .orElseThrow(() -> {
                        log.error("하드웨어 검증 실패 - 존재하지 않는 하드웨어 ID: {}", hardwareId);
                        return new ResourceNotFoundException("지정된 하드웨어를 찾을 수 없습니다.");
                    });
        } else if (serverType == ServerType.PHYSICAL) {
            log.error("하드웨어 검증 실패 - 물리 서버이나 하드웨어 ID가 누락됨");
            throw new IllegalArgumentException("물리 서버(PHYSICAL)는 물리 하드웨어 매핑이 필수입니다.");
        }
        return null; // 가상화/클라우드 서버의 경우 정상적으로 null 반환
    }
}