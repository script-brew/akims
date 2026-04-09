package com.akplaza.infra.domain.network.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.akplaza.infra.domain.network.dto.IpCidrCreateRequest;
import com.akplaza.infra.domain.network.dto.IpCidrDetailResponse;
import com.akplaza.infra.domain.network.dto.IpCidrResponse;
import com.akplaza.infra.domain.network.dto.IpCreateRequest;
import com.akplaza.infra.domain.network.dto.IpResponse;
import com.akplaza.infra.domain.network.entity.AssignedType;
import com.akplaza.infra.domain.network.entity.Ip;
import com.akplaza.infra.domain.network.entity.IpCidr;
import com.akplaza.infra.domain.network.repository.IpCidrRepository;
import com.akplaza.infra.domain.network.repository.IpRepository;
import com.akplaza.infra.domain.device.repository.ServerRepository;
import com.akplaza.infra.domain.hardware.dto.HardwareResponse;
import com.akplaza.infra.domain.hardware.entity.Hardware;
import com.akplaza.infra.domain.device.repository.NetworkDeviceRepository;
import com.akplaza.infra.global.common.repository.DynamicSearchSpec;
import com.akplaza.infra.global.common.util.ExcelUtil;
import com.akplaza.infra.global.error.exception.DuplicateResourceException;
import com.akplaza.infra.global.error.exception.ResourceInUseException;
import com.akplaza.infra.global.error.exception.ResourceNotFoundException;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IpService {

    private final IpRepository ipRepository;
    private final IpCidrRepository ipCidrRepository;
    private final ServerRepository serverRepository;
    private final NetworkDeviceRepository networkDeviceRepository;

    // ==========================================
    // 1. 타 도메인(Server, NetworkDevice)에서 호출하는 핵심 연동 로직
    // ==========================================

    /**
     * 특정 장비에 IP를 할당합니다. (ServerService 등에서 트랜잭션 내에 호출됨)
     */
    @Transactional
    public void allocateIp(Long ipCidrId, String ipAddress, AssignedType targetType, Long targetId) {
        if (ipCidrId == null || ipAddress == null || ipAddress.isBlank())
            return;

        log.debug("IP 할당 프로세스 진입 - IP: {}, 대상 타입: {}, 대상 ID: {}", ipAddress, targetType, targetId);

        // 🚨 1. 방어 로직: IP 대역(CIDR) 존재 여부 및 범위 검증
        IpCidr ipCidr = ipCidrRepository.findById(ipCidrId)
                .orElseThrow(() -> new ResourceNotFoundException("IP 대역을 찾을 수 없습니다."));

        String cidrBlock = ipCidr.getCidrBlock(); // 예: 10.10.10.0/24
        String networkPrefix = cidrBlock.substring(0, cidrBlock.lastIndexOf(".") + 1); // 10.10.10.

        if (!ipAddress.startsWith(networkPrefix)) {
            throw new IllegalArgumentException(
                    String.format("입력한 IP(%s)가 선택한 대역(%s) 범위에 맞지 않습니다.", ipAddress, cidrBlock));
        }

        // 🚨 2. 할당 로직: 중복 검사 및 자동 생성
        java.util.Optional<Ip> optionalIp = ipRepository.findByIpAddress(ipAddress);

        if (optionalIp.isPresent()) {
            Ip ip = optionalIp.get();
            // 다른 장비가 이미 사용 중인지 확인 (자신이 점유한 거면 패스)
            if (ip.isUsed() && !targetId.equals(ip.getAssignedId())) {
                log.error("IP 할당 실패 - 이미 점유된 IP: {} (현재 소유: {})", ipAddress, ip.getAssignedType());
                throw new ResourceInUseException("이미 다른 장비에 할당되어 사용 중인 IP입니다: " + ipAddress);
            }
            // 기존 IP 엔티티 상태 변경
            ip.allocate(targetType, targetId);
            log.info("기존 IP 할당 성공 - IP: {}", ipAddress);
        } else {
            // 시스템에 없는 IP라면 새롭게 생성하여 즉시 할당 (Visual IP Map 렌더링을 위함)
            Ip newIp = Ip.builder()
                    .ipAddress(ipAddress)
                    .isUsed(false) // allocate 메서드에서 true로 바뀜
                    .assignedType(AssignedType.NONE)
                    .build();

            ipCidr.addIp(newIp); // 연관관계 매핑
            newIp.allocate(targetType, targetId); // 할당
            ipRepository.save(newIp);
            log.info("신규 IP 생성 및 할당 성공 - IP: {}", ipAddress);
        }
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

        // 🌟 단건 API 등록 시에도 형식 검증 (화면에서 잘못 넘어오는 것 차단)
        if (!isValidCidrFormat(dto.getCidrBlock())) {
            log.error("CIDR 등록 실패 - 잘못된 형식: {}", dto.getCidrBlock());
            throw new IllegalArgumentException("잘못된 CIDR 형식입니다: " + dto.getCidrBlock());
        }

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

    @Transactional(readOnly = true)
    // 🌟 수정됨: 개별 파라미터 대신 Map<String, String> 을 통째로 받습니다.
    public Page<IpResponse> searchIps(Map<String, String> searchParams, Pageable pageable) {

        // 1. Map을 던져주면 공통 유틸이 동적 WHERE 절(Specification)을 만들어줍니다.
        Specification<Ip> spec = DynamicSearchSpec.searchConditions(searchParams);

        // 2. 만들어진 조건과 페이징 정보를 Repository에 넘깁니다. (메서드는 기본 내장된 findAll 사용)
        Page<Ip> ipPage = ipRepository.findAll(spec, pageable);

        return ipPage.map(IpResponse::new);
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

    public List<IpCidrResponse> getAllIpCidrs() {
        return ipCidrRepository.findAll().stream()
                .map(IpCidrResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    // 🌟 수정됨: 개별 파라미터 대신 Map<String, String> 을 통째로 받습니다.
    public Page<IpCidrResponse> searchIpCidrs(Map<String, String> searchParams, Pageable pageable) {

        // 1. Map을 던져주면 공통 유틸이 동적 WHERE 절(Specification)을 만들어줍니다.
        Specification<IpCidr> spec = DynamicSearchSpec.searchConditions(searchParams);

        // 2. 만들어진 조건과 페이징 정보를 Repository에 넘깁니다. (메서드는 기본 내장된 findAll 사용)
        Page<IpCidr> ipCidrPage = ipCidrRepository.findAll(spec, pageable);
        List<IpCidr> ipCidrs = ipCidrPage.getContent();

        // 결과가 없으면 빈 페이지 반환
        if (ipCidrs.isEmpty()) {
            return new PageImpl<>(java.util.Collections.emptyList(), pageable, ipCidrPage.getTotalElements());
        }

        // 5. 🌟 DTO로 안전하게 조립 (데이터 중복 증식 방지)
        List<IpCidrResponse> responseList = ipCidrs.stream().map(ipCidr -> {
            // ServerResponse 생성자에 엔티티의 연관 객체들을 넘겨주면, DTO 클래스 안에서 필요한 것만 예쁘게 파싱함
            return new IpCidrResponse(ipCidr);
        }).collect(Collectors.toList());

        // return hardwarePage.map(HardwareResponse::new);
        return new PageImpl<>(responseList, pageable, ipCidrPage.getTotalElements());
    }

    // 🌟 추가 2: Visual IP Map을 위한 특정 대역 상세 조회 (IP 목록 포함)
    public IpCidrDetailResponse getIpCidrDetail(Long cidrId) {
        IpCidr ipCidr = ipCidrRepository.findById(cidrId)
                .orElseThrow(() -> new ResourceNotFoundException("IP 대역을 찾을 수 없습니다. ID: " + cidrId));

        // 해당 대역(cidrId)에 속한 IP들만 조회하여 DTO로 변환
        List<IpResponse> ipResponses = ipRepository.findByIpCidrId(cidrId).stream()
                .map(ip -> {
                    IpResponse dto = new IpResponse(ip);

                    // 🌟 15년 차의 디테일: 할당된 장비의 진짜 이름을 조회하여 툴팁에 매핑
                    if (ip.isUsed() && ip.getAssignedId() != null) {
                        if (ip.getAssignedType() == AssignedType.SERVER) {
                            serverRepository.findById(ip.getAssignedId())
                                    .ifPresent(s -> dto.setAssignedTargetName(s.getHostName()));
                        } else if (ip.getAssignedType() == AssignedType.NETWORK_DEVICE) {
                            networkDeviceRepository.findById(ip.getAssignedId())
                                    .ifPresent(nd -> dto.setAssignedTargetName(nd.getName()));
                        }
                    }
                    return dto;
                })
                .collect(Collectors.toList());

        return new IpCidrDetailResponse(ipCidr, ipResponses);
    }

    // 🌟 추가 3: IP 대역 삭제 (하위에 할당된 IP가 있으면 삭제 방지)
    @Transactional
    public void deleteIpCidr(Long cidrId) {
        IpCidr ipCidr = ipCidrRepository.findById(cidrId)
                .orElseThrow(() -> new ResourceNotFoundException("IP 대역을 찾을 수 없습니다."));

        // 대역 내에 누군가 사용 중(isUsed = true)인 IP가 단 하나라도 있다면 삭제 불가!
        boolean hasUsedIps = ipRepository.findByIpCidrId(cidrId).stream()
                .anyMatch(Ip::isUsed);

        if (hasUsedIps) {
            throw new ResourceInUseException("해당 대역 내에 현재 장비에 할당되어 사용 중인 IP가 존재하여 대역을 삭제할 수 없습니다.");
        }

        // 사용 중인 IP가 없다면, 대역 내의 IP 풀을 모두 지우고 대역 자체를 삭제
        ipRepository.deleteByIpCidrId(cidrId);
        ipCidrRepository.delete(ipCidr);
    }

    // ==========================================
    // 3. 엑셀 다운로드 (IP CIDR)
    // ==========================================
    public void downloadExcelIpCidr(Map<String, String> searchParams, HttpServletResponse response) throws Exception {
        log.info("IP 대역 엑셀 다운로드 시작");

        // 검색 조건을 유지한 채로 최대 10,000건 조회
        PageRequest maxPageRequest = PageRequest.of(0, 10000, Sort.by("id").descending());
        Page<IpCidrResponse> cidrPage = this.searchIpCidrs(searchParams, maxPageRequest);

        // 엑셀 헤더 정의
        List<String> headers = Arrays.asList("IP 대역(CIDR Block)", "설명(용도)");

        List<List<Object>> dataList = new ArrayList<>();
        for (IpCidrResponse cidr : cidrPage.getContent()) {
            dataList.add(Arrays.asList(
                    cidr.getCidrBlock(),
                    cidr.getDescription() != null ? cidr.getDescription() : "-"));
        }

        ExcelUtil.download(response, "AKIMS_IP대역_목록", headers, dataList);
        log.info("IP 대역 엑셀 다운로드 완료 (총 {}건)", dataList.size());
    }

    // ==========================================
    // 4. 엑셀 일괄 업로드 (IP CIDR)
    // ==========================================
    @Transactional
    public int uploadExcelIpCidr(MultipartFile file) throws Exception {
        log.info("IP 대역 엑셀 업로드 시작");
        List<List<String>> excelData = ExcelUtil.readExcel(file);
        int successCount = 0;

        // i=1 (0번 인덱스는 헤더이므로 스킵)
        for (int i = 1; i < excelData.size(); i++) {
            List<String> row = excelData.get(i);

            // 데이터가 비정상적이거나 CIDR(1번 열)이 비어있으면 스킵
            if (row.size() < 2 || row.get(1).trim().isEmpty())
                continue;

            try {
                String cidrBlock = row.get(0).trim();

                // 🌟 [추가된 방어 로직] CIDR 형식 엄격 검증
                if (!isValidCidrFormat(cidrBlock)) {
                    log.error("엑셀 업로드 스킵 - 잘못된 CIDR 형식 (행: {}, 입력값: {})", i + 1, cidrBlock);
                    continue; // 잘못된 형식이면 시스템에 넣지 않고 스킵
                }

                // [예외 방어] 이미 등록된 대역이면 무시하고 다음 행으로 진행
                if (ipCidrRepository.existsByCidrBlock(cidrBlock)) {
                    log.warn("엑셀 업로드 스킵 - 이미 존재하는 대역: {}", cidrBlock);
                    continue;
                }

                String description = row.size() > 1 ? row.get(1).trim() : "";

                // Service 내부이므로 DTO를 거치지 않고 바로 안전하게 엔티티 생성 및 영속화
                IpCidr ipCidr = IpCidr.builder()
                        .cidrBlock(cidrBlock)
                        .description(description)
                        .build();

                ipCidrRepository.save(ipCidr);
                successCount++;

            } catch (Exception e) {
                log.error("엑셀 {}번째 행({}) 파싱 실패: {}", i + 1, row.get(1), e.getMessage());
            }
        }
        return successCount;
    }

    // ==========================================
    // 💡 내부 헬퍼 메서드: CIDR 정규식 검증
    // ==========================================
    private boolean isValidCidrFormat(String cidr) {
        // IPv4 주소 (0.0.0.0 ~ 255.255.255.255) + "/" + 서브넷 마스크 (0~32) 를 완벽하게 잡아내는 정규식
        String regex = "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)/(3[0-2]|[1-2]?\\d)$";
        return cidr != null && cidr.matches(regex);
    }
}