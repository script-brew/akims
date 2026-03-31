package com.akplaza.infra.domain.device.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.akplaza.infra.domain.device.dto.DiskCreateRequest;
import com.akplaza.infra.domain.device.dto.DiskResponse;
import com.akplaza.infra.domain.device.dto.DiskUpdateRequest;
import com.akplaza.infra.domain.device.entity.Disk;
import com.akplaza.infra.domain.device.entity.Server;
import com.akplaza.infra.domain.device.repository.DiskRepository;
import com.akplaza.infra.domain.device.repository.ServerRepository;
import com.akplaza.infra.global.error.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiskService {

    private final DiskRepository diskRepository;
    private final ServerRepository serverRepository;

    // ==========================================
    // 1. 디스크 등록 (CREATE)
    // ==========================================
    @Transactional
    public Long createDisk(DiskCreateRequest dto) {
        log.info("디스크 추가 요청 - Target Server ID: {}, Type: {}, Size: {}GB",
                dto.getServerId(), dto.getType(), dto.getSize());

        // [Exception Point] 대상 서버 존재 여부 검증
        Server server = serverRepository.findById(dto.getServerId())
                .orElseThrow(() -> {
                    log.error("디스크 추가 실패 - 존재하지 않는 서버 ID: {}", dto.getServerId());
                    return new ResourceNotFoundException("디스크를 추가할 대상 서버를 찾을 수 없습니다.");
                });

        Disk disk = Disk.builder()
                .type(dto.getType())
                .size(dto.getSize())
                .mountPoint(dto.getMountPoint())
                .build();

        // 🌟 양방향 연관관계 편의 메서드 호출 (객체지향적 영속성 관리)
        server.addDisk(disk);

        Disk savedDisk = diskRepository.save(disk);
        log.info("디스크 추가 완료 - ID: {}", savedDisk.getId());

        return savedDisk.getId();
    }

    // ==========================================
    // 2. 디스크 조회 (READ)
    // ==========================================
    public DiskResponse getDisk(Long id) {
        log.debug("디스크 단건 조회 요청 - ID: {}", id);
        Disk disk = diskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("디스크를 찾을 수 없습니다. ID: " + id));
        return new DiskResponse(disk);
    }

    // 특정 서버의 상세 화면을 볼 때 호출 (해당 서버에 장착된 디스크 목록)
    public List<DiskResponse> getDisksByServerId(Long serverId) {
        log.debug("특정 서버의 디스크 목록 조회 요청 - Server ID: {}", serverId);

        if (!serverRepository.existsById(serverId)) {
            throw new ResourceNotFoundException("서버를 찾을 수 없습니다. ID: " + serverId);
        }

        return diskRepository.findByServerId(serverId).stream()
                .map(DiskResponse::new)
                .collect(Collectors.toList());
    }

    // ==========================================
    // 3. 디스크 수정 (UPDATE)
    // ==========================================
    @Transactional
    public void updateDisk(Long id, DiskUpdateRequest dto) {
        log.info("디스크 수정 트랜잭션 시작 - ID: {}", id);

        Disk disk = diskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("수정할 디스크를 찾을 수 없습니다. ID: " + id));

        // 🌟 엔티티 비즈니스 메서드를 통한 더티 체킹 업데이트
        disk.updateDiskInfo(
                dto.getType(),
                dto.getSize(),
                dto.getMountPoint());

        log.info("디스크 수정 트랜잭션 완료 - ID: {}", id);
    }

    // ==========================================
    // 4. 디스크 삭제 (DELETE)
    // ==========================================
    @Transactional
    public void deleteDisk(Long id) {
        log.info("디스크 삭제 트랜잭션 시작 - ID: {}", id);

        Disk disk = diskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("삭제할 디스크를 찾을 수 없습니다. ID: " + id));

        // 부모(Server) 엔티티와의 양방향 관계를 끊어줍니다.
        if (disk.getServer() != null) {
            disk.getServer().removeDisk(disk);
        }

        diskRepository.delete(disk);
        log.info("디스크 삭제 트랜잭션 완료 - ID: {}", id);
    }
}