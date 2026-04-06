package com.akplaza.infra.domain.device.service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.akplaza.infra.domain.device.dto.NetworkDeviceCreateRequest;
import com.akplaza.infra.domain.device.dto.NetworkDeviceResponse;
import com.akplaza.infra.domain.device.dto.NetworkDeviceUpdateRequest;
import com.akplaza.infra.domain.device.dto.ServerResponse;
import com.akplaza.infra.domain.device.entity.NetworkDevice;
import com.akplaza.infra.domain.device.entity.Server;
import com.akplaza.infra.domain.device.repository.NetworkDeviceRepository;
import com.akplaza.infra.domain.hardware.dto.HardwareResponse;
import com.akplaza.infra.domain.hardware.entity.Hardware;
import com.akplaza.infra.domain.hardware.repository.HardwareRepository;
import com.akplaza.infra.domain.network.dto.IpAssignRequest;
import com.akplaza.infra.domain.network.entity.AssignedType;
import com.akplaza.infra.domain.network.entity.Ip;
import com.akplaza.infra.domain.network.repository.IpRepository;
import com.akplaza.infra.domain.network.service.IpService;
import com.akplaza.infra.global.error.exception.DuplicateResourceException;
import com.akplaza.infra.global.error.exception.ResourceNotFoundException;
import com.akplaza.infra.global.common.repository.DynamicSearchSpec;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본적으로 읽기 전용 (성능 최적화)
public class NetworkDeviceService {

    private final NetworkDeviceRepository networkDeviceRepository;
    private final HardwareRepository hardwareRepository;
    private final IpService ipService; // IP 할당/해제 연동
    private final IpRepository ipRepository; // 조회용

    // ==========================================
    // 1. 장비 생성 (CREATE)
    // ==========================================
    @Transactional
    public Long createNetworkDevice(NetworkDeviceCreateRequest dto) {
        log.info("네트워크 장비 등록 트랜잭션 시작 - Name: {}, Category: {}", dto.getName(), dto.getCategory());

        // [Exception Point 1] 장비명 중복 검증
        if (networkDeviceRepository.existsByName(dto.getName())) {
            log.error("장비 등록 실패 - 중복된 장비명: {}", dto.getName());
            throw new DuplicateResourceException("이미 존재하는 네트워크 장비명입니다: " + dto.getName());
        }

        // [Exception Point 2] 하드웨어 검증
        Hardware hardware = validateAndGetHardware(dto.getHardwareId());

        NetworkDevice device = NetworkDevice.builder()
                .hardware(hardware)
                .name(dto.getName())
                .category(dto.getCategory())
                .os(dto.getOs())
                .description(dto.getDescription())
                .ha(dto.isHa())
                .monitoringInfo(dto.getMonitoringInfo())
                .build();

        NetworkDevice savedDevice = networkDeviceRepository.save(device);
        log.debug("네트워크 장비 기본 정보 저장 완료 - ID: {}", savedDevice.getId());

        // 🌟 N개의 IP 할당 루프
        if (dto.getIps() != null && !dto.getIps().isEmpty()) {
            for (IpAssignRequest ipReq : dto.getIps()) {
                ipService.allocateIp(ipReq.getIpCidrId(), ipReq.getIpAddress(), AssignedType.NETWORK_DEVICE,
                        savedDevice.getId());
            }
        }

        log.info("네트워크 장비 등록 트랜잭션 완료 - ID: {}", savedDevice.getId());
        return savedDevice.getId();
    }

    // ==========================================
    // 2. 장비 조회 (READ)
    // ==========================================
    public NetworkDeviceResponse getNetworkDevice(Long id) {
        log.debug("네트워크 장비 단건 조회 요청 - ID: {}", id);

        NetworkDevice device = networkDeviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("장비를 찾을 수 없습니다. ID: " + id));

        List<Ip> assignedIp = ipRepository.findByAssignedTypeAndAssignedId(AssignedType.NETWORK_DEVICE, device.getId());
        return new NetworkDeviceResponse(device, assignedIp);
    }

    @Transactional(readOnly = true)
    // 🌟 수정됨: 개별 파라미터 대신 Map<String, String> 을 통째로 받습니다.
    public Page<NetworkDeviceResponse> searchNetworkDevices(Map<String, String> searchParams, Pageable pageable) {

        // 1. Map을 던져주면 공통 유틸이 동적 WHERE 절(Specification)을 만들어줍니다.
        Specification<NetworkDevice> spec = DynamicSearchSpec.searchConditions(searchParams);

        // 2. 만들어진 조건과 페이징 정보를 Repository에 넘깁니다. (메서드는 기본 내장된 findAll 사용)
        Page<NetworkDevice> networkDevicePage = networkDeviceRepository.findAll(spec, pageable);
        List<NetworkDevice> networkDevices = networkDevicePage.getContent();

        // 결과가 없으면 빈 페이지 반환
        if (networkDevices.isEmpty()) {
            return new PageImpl<>(java.util.Collections.emptyList(), pageable, networkDevicePage.getTotalElements());
        }

        // 3. 🚨 N+1 방어 로직: 조회된 서버 ID들만 추출
        List<Long> networkDeviceIds = networkDevices.stream().map(NetworkDevice::getId).collect(Collectors.toList());

        // 4. IP 목록 한 번에 조회 후 서버 ID를 키값으로 그룹핑 (Map 생성)
        List<Ip> allIps = ipRepository.findByAssignedTypeAndAssignedIdIn(AssignedType.NETWORK_DEVICE, networkDeviceIds);
        Map<Long, List<Ip>> ipMap = allIps.stream().collect(Collectors.groupingBy(Ip::getAssignedId));

        // 5. 🌟 DTO로 안전하게 조립 (데이터 중복 증식 방지)
        List<NetworkDeviceResponse> responseList = networkDevices.stream().map(networkDevice -> {
            List<Ip> mappedIps = ipMap.getOrDefault(networkDevice.getId(), java.util.Collections.emptyList());

            // ServerResponse 생성자에 엔티티의 연관 객체들을 넘겨주면, DTO 클래스 안에서 필요한 것만 예쁘게 파싱함
            return new NetworkDeviceResponse(networkDevice, mappedIps);
        }).collect(Collectors.toList());

        return new PageImpl<>(responseList, pageable, networkDevicePage.getTotalElements());
    }

    public List<NetworkDeviceResponse> getAllNetworkDevices() {
        log.debug("전체 네트워크 장비 목록 조회 요청");
        List<NetworkDevice> devices = networkDeviceRepository.findAllWithHardwareAndRack();
        if (devices.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        // 🌟 15년 차의 최적화: N+1 문제 해결을 위해 IN 쿼리 단 1번으로 모든 할당 IP 조회
        List<Long> deviceIds = devices.stream().map(NetworkDevice::getId).collect(Collectors.toList());

        // (IpRepository에 List<Ip> findByAssignedTypeAndAssignedIdIn(...) 메서드 추가 필요)
        List<Ip> allIps = ipRepository.findByAssignedTypeAndAssignedIdIn(AssignedType.NETWORK_DEVICE, deviceIds);

        // 메모리에서 DeviceId를 Key로 하는 Map 생성 (O(1) 매핑)
        java.util.Map<Long, List<Ip>> ipMap = allIps.stream()
                .collect(Collectors.groupingBy(Ip::getAssignedId));

        return devices.stream().map(device -> {
            List<Ip> mappedIps = ipMap.getOrDefault(device.getId(), java.util.Collections.emptyList());
            // DTO에 엔티티와 할당된 Ip 객체 자체를 넘겨서 DTO 내부에서 ipAddress와 ipCidrId를 추출하게 함
            return new NetworkDeviceResponse(device, mappedIps);
        }).collect(Collectors.toList());
    }

    // ==========================================
    // 3. 장비 수정 (UPDATE)
    // ==========================================
    @Transactional
    public void updateNetworkDevice(Long id, NetworkDeviceUpdateRequest dto) {
        log.info("네트워크 장비 수정 트랜잭션 시작 - ID: {}", id);

        NetworkDevice device = networkDeviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("수정할 장비를 찾을 수 없습니다. ID: " + id));

        // 이름 변경 시 중복 체크
        if (!device.getName().equals(dto.getName()) && networkDeviceRepository.existsByName(dto.getName())) {
            log.error("장비 수정 실패 - 변경하려는 장비명이 이미 존재함: {}", dto.getName());
            throw new DuplicateResourceException("이미 존재하는 네트워크 장비명입니다: " + dto.getName());
        }

        Hardware newHardware = validateAndGetHardware(dto.getHardwareId());

        // 🌟 추가된 부분: IP 정보가 변경되었을 수 있으므로, 기존 IP 해제 후 새 IP 재할당
        ipService.releaseIpForTarget(AssignedType.NETWORK_DEVICE, device.getId());

        // 🌟 N개의 IP 할당 루프
        if (dto.getIps() != null && !dto.getIps().isEmpty()) {
            for (IpAssignRequest ipReq : dto.getIps()) {
                ipService.allocateIp(ipReq.getIpCidrId(), ipReq.getIpAddress(), AssignedType.NETWORK_DEVICE,
                        device.getId());
            }
        }

        device.updateDeviceInfo(
                dto.getName(),
                dto.getCategory(),
                dto.getOs(),
                dto.getDescription(),
                dto.isHa(),
                dto.getMonitoringInfo(),
                newHardware);

        log.info("네트워크 장비 수정 트랜잭션 완료 - ID: {}", id);
    }

    // ==========================================
    // 4. 장비 삭제 (DELETE)
    // ==========================================
    @Transactional
    public void deleteNetworkDevice(Long id) {
        log.info("네트워크 장비 삭제 트랜잭션 시작 - ID: {}", id);

        NetworkDevice device = networkDeviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("삭제할 장비를 찾을 수 없습니다. ID: " + id));

        // [비즈니스 로직 연계] 삭제 전 점유 중이던 IP 회수(Release)
        log.debug("네트워크 장비 삭제 전, 할당된 IP 반환 처리 실행");
        ipService.releaseIpForTarget(AssignedType.NETWORK_DEVICE, device.getId());

        networkDeviceRepository.delete(device);
        log.info("네트워크 장비 삭제 트랜잭션 완료 - ID: {}", id);
    }

    // ==========================================
    // 내부 헬퍼 메서드
    // ==========================================
    private Hardware validateAndGetHardware(Long hardwareId) {
        if (hardwareId == null)
            return null; // 가상 네트워크 어플라이언스(vFW 등) 허용

        return hardwareRepository.findById(hardwareId)
                .orElseThrow(() -> {
                    log.error("하드웨어 검증 실패 - 존재하지 않는 하드웨어 ID: {}", hardwareId);
                    return new ResourceNotFoundException("지정된 물리 하드웨어를 찾을 수 없습니다.");
                });
    }
}