package com.akplaza.infra.domain.hardware.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.akplaza.infra.domain.device.repository.NetworkDeviceRepository;
import com.akplaza.infra.domain.device.repository.ServerRepository;
import com.akplaza.infra.domain.hardware.dto.HardwareCreateRequest;
import com.akplaza.infra.domain.hardware.dto.HardwareResponse;
import com.akplaza.infra.domain.hardware.dto.HardwareUpdateRequest;
import com.akplaza.infra.domain.hardware.entity.Hardware;
import com.akplaza.infra.domain.hardware.repository.HardwareRepository;
import com.akplaza.infra.domain.space.entity.Rack;
import com.akplaza.infra.domain.space.repository.RackRepository;
import com.akplaza.infra.global.error.exception.DuplicateResourceException;
import com.akplaza.infra.global.error.exception.ResourceInUseException;
import com.akplaza.infra.global.error.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HardwareService {

    private final HardwareRepository hardwareRepository;
    private final RackRepository rackRepository;
    private final ServerRepository serverRepository;
    private final NetworkDeviceRepository networkDeviceRepository;

    // ==========================================
    // 1. 하드웨어 등록 (CREATE)
    // ==========================================
    @Transactional
    public Long createHardware(HardwareCreateRequest dto) {
        log.info("하드웨어 등록 트랜잭션 시작 - Model: {}, Serial: {}", dto.getModel(), dto.getSerialNo());

        // [Exception Point 1] Serial Number 중복 검사 (물리 장비의 절대 식별자)
        if (hardwareRepository.existsBySerialNo(dto.getSerialNo())) {
            log.error("하드웨어 등록 실패 - 중복된 Serial Number: {}", dto.getSerialNo());
            throw new DuplicateResourceException("이미 등록된 시리얼 넘버입니다: " + dto.getSerialNo());
        }

        // [Exception Point 2] Rack 매핑 검증
        Rack rack = getRackIfPresent(dto.getRackId());

        Hardware hardware = Hardware.builder()
                .rack(rack)
                .rackPosition(dto.getRackPosition())
                .size(dto.getSize())
                .equipmentType(dto.getEquipmentType())
                .introductionYear(dto.getIntroductionYear())
                .model(dto.getModel())
                .serialNo(dto.getSerialNo())
                .description(dto.getDescription())
                .build();

        Hardware savedHardware = hardwareRepository.save(hardware);
        log.info("하드웨어 등록 완료 - ID: {}", savedHardware.getId());

        return savedHardware.getId();
    }

    // ==========================================
    // 2. 하드웨어 조회 (READ)
    // ==========================================
    public HardwareResponse getHardware(Long id) {
        log.debug("하드웨어 단건 조회 요청 - ID: {}", id);
        Hardware hardware = hardwareRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("하드웨어를 찾을 수 없습니다. ID: " + id));
        return new HardwareResponse(hardware);
    }

    public List<HardwareResponse> getAllHardwares() {
        log.debug("전체 하드웨어 목록 조회 요청");
        // N+1 문제 방지를 위해 Rack과 Location을 Fetch Join하는 메서드를 Repository에 추가 권장
        return hardwareRepository.findAll().stream()
                .map(HardwareResponse::new)
                .collect(Collectors.toList());
    }

    // ==========================================
    // 3. 하드웨어 수정 (UPDATE)
    // ==========================================
    @Transactional
    public void updateHardware(Long id, HardwareUpdateRequest dto) {
        log.info("하드웨어 수정 트랜잭션 시작 - ID: {}", id);

        Hardware hardware = hardwareRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("수정할 하드웨어를 찾을 수 없습니다. ID: " + id));

        // Serial Number가 변경된 경우 중복 체크
        if (!hardware.getSerialNo().equals(dto.getSerialNo()) &&
                hardwareRepository.existsBySerialNo(dto.getSerialNo())) {
            log.error("하드웨어 수정 실패 - 변경하려는 Serial Number가 이미 존재함: {}", dto.getSerialNo());
            throw new DuplicateResourceException("이미 존재하는 시리얼 넘버입니다: " + dto.getSerialNo());
        }

        Rack newRack = getRackIfPresent(dto.getRackId());

        // 💡 엔티티 상태 변경 (더티 체킹) - 빌더 패턴 외에 엔티티 내부 Update 메서드 활용 권장
        // 하드웨어 교체나 데이터센터 이전 시 Rack 정보가 변경될 수 있음
        hardware.updateHardwareInfo(dto.getModel(), dto.getSerialNo(), dto.getEquipmentType(),
                dto.getIntroductionYear(), dto.getSize(), dto.getDescription(),
                newRack, dto.getRackPosition());

        log.info("하드웨어 수정 트랜잭션 완료 - ID: {}", id);
    }

    // ==========================================
    // 4. 하드웨어 삭제 (DELETE)
    // ==========================================
    @Transactional
    public void deleteHardware(Long id) {
        log.info("하드웨어 삭제 트랜잭션 시작 - ID: {}", id);

        Hardware hardware = hardwareRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("삭제할 하드웨어를 찾을 수 없습니다. ID: " + id));

        // 🌟 [Exception Point 3] 참조 무결성 검증 (매우 중요)
        // 물리 장비를 폐기(삭제)하려는데, 시스템상으로는 해당 장비에서 아직 서버나 네트워크 인스턴스가 돌고 있다면?
        if (serverRepository.existsByHardwareId(id)) {
            log.error("하드웨어 삭제 실패 - 매핑된 서버 존재. Hardware ID: {}", id);
            throw new ResourceInUseException("해당 하드웨어에 등록된 서버 인스턴스가 존재하여 삭제할 수 없습니다.");
        }

        if (networkDeviceRepository.existsByHardwareId(id)) {
            log.error("하드웨어 삭제 실패 - 매핑된 네트워크 장비 존재. Hardware ID: {}", id);
            throw new ResourceInUseException("해당 하드웨어에 등록된 네트워크 장비가 존재하여 삭제할 수 없습니다.");
        }

        hardwareRepository.delete(hardware);
        log.info("하드웨어 삭제 트랜잭션 완료 - ID: {}", id);
    }

    // ==========================================
    // 내부 헬퍼 메서드
    // ==========================================
    private Rack getRackIfPresent(Long rackId) {
        if (rackId == null)
            return null; // 창고에 보관 중인 미실장 장비일 수 있음

        return rackRepository.findById(rackId)
                .orElseThrow(() -> {
                    log.error("Rack 매핑 실패 - 존재하지 않는 Rack ID: {}", rackId);
                    return new ResourceNotFoundException("지정된 Rack을 찾을 수 없습니다.");
                });
    }
}