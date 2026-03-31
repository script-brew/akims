package com.akplaza.infra.domain.device.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.akplaza.infra.domain.device.dto.NetworkDeviceCreateRequest;
import com.akplaza.infra.domain.device.dto.NetworkDeviceResponse;
import com.akplaza.infra.domain.device.dto.NetworkDeviceUpdateRequest;
import com.akplaza.infra.domain.device.entity.NetworkDevice;
import com.akplaza.infra.domain.device.repository.NetworkDeviceRepository;
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
        Hardware hardware = getHardwareIfPresent(dto.getHardwareId());

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

        // [비즈니스 로직 연계] IP 할당 처리
        if (dto.getIpAddresses() != null && !dto.getIpAddresses().isEmpty()) {
            log.debug("네트워크 장비 IP 할당 로직 실행 - 대상 IP 수: {}", dto.getIpAddresses().size());
            for (String ipAddress : dto.getIpAddresses()) {
                // IpService가 트랜잭션에 참여. 실패 시 장비 등록까지 롤백됨.
                ipService.allocateIp(ipAddress, AssignedType.NETWORK_DEVICE, savedDevice.getId());
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

        List<String> assignedIps = getAssignedIpAddresses(id);
        return new NetworkDeviceResponse(device, assignedIps);
    }

    public List<NetworkDeviceResponse> getAllNetworkDevices() {
        log.debug("전체 네트워크 장비 목록 조회 요청");
        // FETCH JOIN으로 N+1 문제 해결 (Repository에 구현된 쿼리 사용)
        List<NetworkDevice> devices = networkDeviceRepository.findAllWithHardwareAndRack();

        return devices.stream().map(device -> {
            List<String> assignedIps = getAssignedIpAddresses(device.getId());
            return new NetworkDeviceResponse(device, assignedIps);
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

        Hardware newHardware = getHardwareIfPresent(dto.getHardwareId());

        // 💡 실무 권장: 엔티티 내부에 update(...) 메서드를 만들어서 사용하지만, 현재 빌더만 있다면
        // 하드웨어 변경을 처리하기 위해 엔티티에 메서드를 추가하는 것이 좋습니다.
        // 현재는 하드웨어와 같은 핵심 연관관계만 더티 체킹의 예시로 둡니다.
        // device.update(dto.getName(), dto.getCategory(), dto.getOs(), ... ,
        // newHardware);

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
    private Hardware getHardwareIfPresent(Long hardwareId) {
        if (hardwareId == null)
            return null; // 가상 네트워크 어플라이언스(vFW 등) 허용

        return hardwareRepository.findById(hardwareId)
                .orElseThrow(() -> {
                    log.error("하드웨어 검증 실패 - 존재하지 않는 하드웨어 ID: {}", hardwareId);
                    return new ResourceNotFoundException("지정된 물리 하드웨어를 찾을 수 없습니다.");
                });
    }

    private List<String> getAssignedIpAddresses(Long deviceId) {
        return ipRepository.findByAssignedTypeAndAssignedId(AssignedType.NETWORK_DEVICE, deviceId)
                .stream()
                .map(Ip::getIpAddress)
                .collect(Collectors.toList());
    }
}