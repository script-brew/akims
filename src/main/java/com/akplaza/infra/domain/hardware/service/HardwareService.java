package com.akplaza.infra.domain.hardware.service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

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
import com.akplaza.infra.global.common.repository.DynamicSearchSpec;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.jpa.domain.Specification;

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
    // 🌟 물리적 실장(Rack Mounting) 유효성 검증 로직
    // ==========================================
    private void validateRackSpace(Rack rack, Integer rackPosition, Integer size, Long currentHardwareId) {
        if (rack == null || rackPosition == null)
            return; // 미실장(창고 보관)인 경우 패스

        // 1. 랙 범위(Size) 초과 검사
        // 예: 42U 랙에 40U 위치부터 4U 짜리를 넣으면 끝 위치가 43U가 되어 에러 발생
        int endU = rackPosition + size - 1;
        if (rackPosition < 1 || endU > rack.getSize()) {
            throw new IllegalArgumentException(
                    String.format("장비 크기 초과: 해당 랙(%s)은 %dU까지 실장 가능하며, 입력하신 장비는 %dU ~ %dU 공간을 요구합니다.",
                            rack.getRackNo(), rack.getSize(), rackPosition, endU));
        }

        // 2. 다른 장비와의 겹침(충돌) 검사
        boolean isOverlapping = hardwareRepository.existsOverlappingHardware(
                rack.getId(), rackPosition, endU, currentHardwareId);
        if (isOverlapping) {
            throw new ResourceInUseException(
                    String.format("실장 위치 충돌: %s 랙의 %dU ~ %dU 위치에는 이미 다른 장비가 실장되어 있습니다.",
                            rack.getRackNo(), rackPosition, endU));
        }
    }

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

        Rack rack = null;
        Integer finalRackPosition = null;
        if (dto.getRackId() != null) {
            rack = rackRepository.findById(dto.getRackId())
                    .orElseThrow(() -> new ResourceNotFoundException("지정된 랙을 찾을 수 없습니다."));

            // 🌟 등록 전 공간 검증 수행 (새로 생성하므로 currentHardwareId는 null)
            validateRackSpace(rack, dto.getRackPosition(), dto.getSize(), null);
            finalRackPosition = dto.getRackPosition(); // 랙이 있을 때만 높이 인정
        }

        Hardware hardware = Hardware.builder()
                .rack(rack)
                .rackPosition(finalRackPosition)
                .size(dto.getSize())
                .equipmentType(dto.getEquipmentType())
                .introductionYear(dto.getIntroductionYear())
                .model(dto.getModel())
                .serialNo(dto.getSerialNo())
                .isSinglePower(dto.getIsSinglePower())
                .powerLine(dto.getPowerLine())
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

    @Transactional(readOnly = true)
    // 🌟 수정됨: 개별 파라미터 대신 Map<String, String> 을 통째로 받습니다.
    public Page<HardwareResponse> searchHardwares(Map<String, String> searchParams, Pageable pageable) {

        // 1. Map을 던져주면 공통 유틸이 동적 WHERE 절(Specification)을 만들어줍니다.
        Specification<Hardware> spec = DynamicSearchSpec.searchConditions(searchParams);

        // 2. 만들어진 조건과 페이징 정보를 Repository에 넘깁니다. (메서드는 기본 내장된 findAll 사용)
        Page<Hardware> hardwarePage = hardwareRepository.findAll(spec, pageable);
        List<Hardware> hardwares = hardwarePage.getContent();

        // 결과가 없으면 빈 페이지 반환
        if (hardwares.isEmpty()) {
            return new PageImpl<>(java.util.Collections.emptyList(), pageable, hardwarePage.getTotalElements());
        }

        // 5. 🌟 DTO로 안전하게 조립 (데이터 중복 증식 방지)
        List<HardwareResponse> responseList = hardwares.stream().map(hardware -> {
            // ServerResponse 생성자에 엔티티의 연관 객체들을 넘겨주면, DTO 클래스 안에서 필요한 것만 예쁘게 파싱함
            return new HardwareResponse(hardware);
        }).collect(Collectors.toList());

        // return hardwarePage.map(HardwareResponse::new);
        return new PageImpl<>(responseList, pageable, hardwarePage.getTotalElements());
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

        Rack newRack = null;
        Integer finalRackPosition = null;
        if (dto.getRackId() != null) {
            newRack = rackRepository.findById(dto.getRackId())
                    .orElseThrow(() -> new ResourceNotFoundException("지정된 랙을 찾을 수 없습니다."));

            // 🌟 수정 전 공간 검증 수행 (자기 자신의 ID는 충돌 검사에서 제외)
            validateRackSpace(newRack, dto.getRackPosition(), dto.getSize(), id);
            finalRackPosition = dto.getRackPosition(); // 랙이 있을 때만 높이 인정
        }

        // 💡 엔티티 상태 변경 (더티 체킹) - 빌더 패턴 외에 엔티티 내부 Update 메서드 활용 권장
        // 하드웨어 교체나 데이터센터 이전 시 Rack 정보가 변경될 수 있음
        hardware.updateHardwareInfo(dto.getModel(), dto.getSerialNo(), dto.getEquipmentType(),
                dto.getIntroductionYear(), dto.getSize(), dto.getDescription(),
                newRack, finalRackPosition, dto.getIsSinglePower(), dto.getPowerLine());

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