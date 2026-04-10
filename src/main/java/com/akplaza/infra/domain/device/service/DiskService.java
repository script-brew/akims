package com.akplaza.infra.domain.device.service;

import java.util.Map;

import com.akplaza.infra.domain.device.dto.DiskCreateRequest;
import com.akplaza.infra.domain.device.dto.DiskRequest;
import com.akplaza.infra.domain.device.dto.DiskResponse;
import com.akplaza.infra.domain.device.dto.DiskUpdateRequest;
import com.akplaza.infra.domain.device.entity.Disk;
import com.akplaza.infra.domain.device.entity.Server;
import com.akplaza.infra.domain.device.repository.DiskRepository;
import com.akplaza.infra.domain.device.repository.ServerRepository;
import com.akplaza.infra.global.error.exception.ResourceNotFoundException;
import com.akplaza.infra.global.common.repository.DynamicSearchSpec;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.jpa.domain.Specification;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiskService {

    private final DiskRepository diskRepository;
    private final ServerRepository serverRepository;

    /**
     * 🌟 서버에 N개의 디스크를 동기화하여 할당합니다. (ServerService에서 트랜잭션 내에 호출됨)
     */
    @Transactional
    public void syncDisksToServer(Long serverId, List<DiskRequest> diskRequests) {
        // 1. 기존 서버에 물려있던 디스크를 모두 해제(삭제)합니다.
        diskRepository.deleteByServerId(serverId); // 🚨 DiskRepository에 이 메서드를 만들어야 합니다!

        // 2. 프론트엔드에서 넘어온 디스크 목록이 없으면 종료
        if (diskRequests == null || diskRequests.isEmpty())
            return;

        // 3. 새 디스크 목록을 생성하여 저장합니다.
        Server server = serverRepository.findById(serverId).orElseThrow();

        for (DiskRequest req : diskRequests) {
            Disk disk = Disk.builder()
                    .server(server)
                    .diskType(req.getDiskType())
                    .size(req.getSize())
                    .diskName(req.getDiskName())
                    .build();
            diskRepository.save(disk);
        }
    }

    public List<DiskResponse> getAllDisks() {
        return diskRepository.findAll().stream()
                .map(DiskResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public Long createDisk(DiskCreateRequest dto) {
        Server server = serverRepository.findById(dto.getServerId())
                .orElseThrow(() -> new ResourceNotFoundException("서버를 찾을 수 없습니다."));
        Disk disk = Disk.builder()
                .server(server)
                .diskType(dto.getDiskType())
                .size(dto.getSize())
                .diskName(dto.getDiskName())
                .build();
        return diskRepository.save(disk).getId();
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

    @Transactional(readOnly = true)
    // 🌟 수정됨: 개별 파라미터 대신 Map<String, String> 을 통째로 받습니다.
    public Page<DiskResponse> searchDisks(Map<String, String> searchParams, Pageable pageable) {

        // 1. Map을 던져주면 공통 유틸이 동적 WHERE 절(Specification)을 만들어줍니다.
        Specification<Disk> spec = DynamicSearchSpec.searchConditions(searchParams);

        // 2. 만들어진 조건과 페이징 정보를 Repository에 넘깁니다. (메서드는 기본 내장된 findAll 사용)
        Page<Disk> diskPage = diskRepository.findAll(spec, pageable);

        return diskPage.map(DiskResponse::new);
    }

    public List<DiskResponse> getDisksByServerId(Long serverId) {
        log.debug("디스크 단건 조회 요청 - ID: {}", serverId);

        return diskRepository.findByServerId(serverId).stream()
                .map(DiskResponse::new)
                .collect(Collectors.toList());
    }

    // ==========================================
    // 3. 디스크 수정 (UPDATE)
    @Transactional
    public void updateDisk(Long id, DiskUpdateRequest dto) {
        Disk disk = diskRepository.findById(id).orElseThrow();
        disk.updateDiskInfo(dto.getDiskType(), dto.getSize(), dto.getMountPoint());
    }

    @Transactional
    public void deleteDisk(Long id) {
        diskRepository.deleteById(id);
    }
}