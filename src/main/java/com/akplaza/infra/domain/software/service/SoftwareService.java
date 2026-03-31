package com.akplaza.infra.domain.software.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.akplaza.infra.domain.device.entity.Server;
import com.akplaza.infra.domain.device.repository.ServerRepository;
import com.akplaza.infra.domain.software.dto.SoftwareCreateRequest;
import com.akplaza.infra.domain.software.dto.SoftwareResponse;
import com.akplaza.infra.domain.software.dto.SoftwareUpdateRequest;
import com.akplaza.infra.domain.software.entity.Software;
import com.akplaza.infra.domain.software.repository.SoftwareRepository;
import com.akplaza.infra.global.error.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SoftwareService {

    private final SoftwareRepository softwareRepository;
    private final ServerRepository serverRepository;

    // ==========================================
    // 1. 소프트웨어 등록 (CREATE)
    // ==========================================
    @Transactional
    public Long createSoftware(SoftwareCreateRequest dto) {
        log.info("소프트웨어 등록 요청 - Name: {}, Target Server ID: {}", dto.getName(), dto.getServerId());

        // [Exception Point] 대상 서버 존재 여부 검증
        Server server = serverRepository.findById(dto.getServerId())
                .orElseThrow(() -> {
                    log.error("소프트웨어 등록 실패 - 존재하지 않는 서버 ID: {}", dto.getServerId());
                    return new ResourceNotFoundException("설치 대상 서버를 찾을 수 없습니다.");
                });

        Software software = Software.builder()
                .name(dto.getName())
                .version(dto.getVersion())
                .purpose(dto.getPurpose())
                .maintenanceInfo(dto.getMaintenanceInfo())
                .build();

        // 🌟 양방향 연관관계 편의 메서드 호출 (Server 엔티티에 있는 addSoftware 사용)
        server.addSoftware(software);

        Software savedSoftware = softwareRepository.save(software);
        log.info("소프트웨어 등록 완료 - ID: {}", savedSoftware.getId());

        return savedSoftware.getId();
    }

    // ==========================================
    // 2. 소프트웨어 조회 (READ)
    // ==========================================
    public SoftwareResponse getSoftware(Long id) {
        log.debug("소프트웨어 단건 조회 요청 - ID: {}", id);
        Software software = softwareRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("소프트웨어를 찾을 수 없습니다. ID: " + id));
        return new SoftwareResponse(software);
    }

    // 🌟 특정 서버의 상세 화면을 볼 때 호출되는 핵심 메서드
    public List<SoftwareResponse> getSoftwareByServerId(Long serverId) {
        log.debug("특정 서버에 설치된 소프트웨어 목록 조회 요청 - Server ID: {}", serverId);

        // 서버 존재 여부 선행 검증
        if (!serverRepository.existsById(serverId)) {
            throw new ResourceNotFoundException("서버를 찾을 수 없습니다. ID: " + serverId);
        }

        return softwareRepository.findByServerId(serverId).stream()
                .map(SoftwareResponse::new)
                .collect(Collectors.toList());
    }

    // ==========================================
    // 3. 소프트웨어 수정 (UPDATE)
    // ==========================================
    @Transactional
    public void updateSoftware(Long id, SoftwareUpdateRequest dto) {
        log.info("소프트웨어 수정 트랜잭션 시작 - ID: {}", id);

        Software software = softwareRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("수정할 소프트웨어를 찾을 수 없습니다. ID: " + id));

        // 엔티티 비즈니스 메서드를 통한 더티 체킹 업데이트
        software.updateSoftwareInfo(
                dto.getName(),
                dto.getVersion(),
                dto.getPurpose(),
                dto.getMaintenanceInfo());

        log.info("소프트웨어 수정 트랜잭션 완료 - ID: {}", id);
    }

    // ==========================================
    // 4. 소프트웨어 삭제 (DELETE)
    // ==========================================
    @Transactional
    public void deleteSoftware(Long id) {
        log.info("소프트웨어 삭제 트랜잭션 시작 - ID: {}", id);

        Software software = softwareRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("삭제할 소프트웨어를 찾을 수 없습니다. ID: " + id));

        // 양방향 관계를 끊어주는 작업 (객체지향적 영속성 관리)
        if (software.getServer() != null) {
            software.getServer().removeSoftware(software);
        }

        softwareRepository.delete(software);
        log.info("소프트웨어 삭제 트랜잭션 완료 - ID: {}", id);
    }
}