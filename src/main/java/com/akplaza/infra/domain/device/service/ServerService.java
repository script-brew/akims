package com.akplaza.infra.domain.device.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.akplaza.infra.domain.device.dto.ServerCreateRequest;
import com.akplaza.infra.domain.device.dto.ServerResponse;
import com.akplaza.infra.domain.device.dto.ServerUpdateRequest;
import com.akplaza.infra.domain.device.entity.Server;
import com.akplaza.infra.domain.device.entity.ServerSpec;
import com.akplaza.infra.domain.device.entity.ServerType;
import com.akplaza.infra.domain.device.repository.ServerRepository;
import com.akplaza.infra.domain.hardware.entity.Hardware;
import com.akplaza.infra.domain.hardware.repository.HardwareRepository;
import com.akplaza.infra.domain.network.entity.AssignedType;
import com.akplaza.infra.domain.network.entity.Ip;
import com.akplaza.infra.domain.network.repository.IpRepository;
import com.akplaza.infra.domain.network.service.IpService;
import com.akplaza.infra.global.error.exception.DuplicateResourceException;
import com.akplaza.infra.global.error.exception.ResourceNotFoundException;

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
                .name(dto.getHostName())
                .category(dto.getCategory())
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

        // [비즈니스 로직 연계] IP 할당 처리
        if (dto.getIpAddresses() != null && !dto.getIpAddresses().isEmpty()) {
            log.debug("서버 IP 할당 로직 실행 - 요청 IP 수: {}", dto.getIpAddresses().size());
            for (String ipAddress : dto.getIpAddresses()) {
                // IpService에 위임. 만약 누군가 쓰고있는 IP라면 여기서 예외가 터지고 트랜잭션 전체가 롤백됨.
                ipService.allocateIp(ipAddress, AssignedType.SERVER, savedServer.getId());
            }
        }

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
        List<String> assignedIps = getAssignedIpAddresses(id);

        return new ServerResponse(server, assignedIps);
    }

    public List<ServerResponse> getAllServers() {
        log.debug("전체 서버 목록 조회 요청");
        // FETCH JOIN이 적용된 Repository 메서드를 사용하여 N+1 쿼리 문제 방지
        List<Server> servers = serverRepository.findAllWithHardwareAndRack();

        return servers.stream().map(server -> {
            List<String> assignedIps = getAssignedIpAddresses(server.getId());
            return new ServerResponse(server, assignedIps);
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
        if (!server.getName().equals(dto.getHostName()) && serverRepository.existsByHostName(dto.getHostName())) {
            log.error("서버 수정 실패 - 변경하려는 HostName이 이미 존재함: {}", dto.getHostName());
            throw new DuplicateResourceException("이미 존재하는 서버명입니다: " + dto.getHostName());
        }

        // [예외 포인트 4] 하드웨어 변경 처리
        Hardware newHardware = validateAndGetHardware(dto.getHardwareId(), server.getServerType());

        server.updateServerInfo(
                dto.getHostName(),
                dto.getCategory(),
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

    // 특정 대상(서버)에 할당된 IP 문자열 리스트 추출
    private List<String> getAssignedIpAddresses(Long serverId) {
        return ipRepository.findByAssignedTypeAndAssignedId(AssignedType.SERVER, serverId)
                .stream()
                .map(Ip::getIpAddress)
                .collect(Collectors.toList());
    }
}