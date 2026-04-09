package com.akplaza.infra.domain.hardware.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.akplaza.infra.domain.hardware.dto.HardwareCreateRequest;
import com.akplaza.infra.domain.hardware.dto.HardwareResponse;
import com.akplaza.infra.domain.hardware.dto.HardwareUpdateRequest;
import com.akplaza.infra.domain.hardware.entity.EquipmentType;
import com.akplaza.infra.domain.hardware.service.HardwareService;
import com.akplaza.infra.domain.hardware.repository.HardwareRepository;
import com.akplaza.infra.domain.space.entity.Rack;
import com.akplaza.infra.domain.space.repository.RackRepository;
import com.akplaza.infra.global.common.util.ExcelUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "5. Hardware API", description = "데이터센터 물리 하드웨어(서버, 스위치, 스토리지 장비 등) 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/hardwares")
@RequiredArgsConstructor
public class HardwareController {

    private final HardwareService hardwareService;
    private final RackRepository rackRepository;

    @Operation(summary = "하드웨어 장비 등록", description = "새로운 물리 하드웨어 장비를 시스템에 등록합니다. (랙 실장 정보 포함 가능)")
    @PostMapping
    public ResponseEntity<Long> createHardware(@Valid @RequestBody HardwareCreateRequest request) {
        log.info("Hardware API: 등록 요청 - SerialNo: {}, Model: {}", request.getSerialNo(), request.getModel());
        Long hardwareId = hardwareService.createHardware(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(hardwareId);
    }

    @Operation(summary = "하드웨어 단건 조회", description = "특정 하드웨어 장비의 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<HardwareResponse> getHardware(@PathVariable Long id) {
        log.debug("Hardware API: 단건 조회 요청 - ID: {}", id);
        HardwareResponse response = hardwareService.getHardware(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<HardwareResponse>> getHardwares(
            @RequestParam Map<String, String> searchParams, // 🌟 핵심: 프론트가 보내는 모든 필터를 Map으로 흡수
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("id").descending());

        return ResponseEntity.ok(hardwareService.searchHardwares(searchParams, pageRequest));
    }

    // @Operation(summary = "하드웨어 전체 조회", description = "시스템에 등록된 전체 물리 하드웨어 목록을
    // 조회합니다.")
    // @GetMapping
    // public ResponseEntity<List<HardwareResponse>> getAllHardwares() {
    // log.debug("Hardware API: 전체 목록 조회 요청");
    // List<HardwareResponse> responses = hardwareService.getAllHardwares();
    // return ResponseEntity.ok(responses);
    // }

    @Operation(summary = "하드웨어 정보 수정", description = "특정 하드웨어의 정보(랙 위치 변경, 설명 수정 등)를 업데이트합니다.")
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateHardware(
            @PathVariable Long id,
            @Valid @RequestBody HardwareUpdateRequest request) {
        log.info("Hardware API: 수정 요청 - ID: {}", id);
        hardwareService.updateHardware(id, request);
        return ResponseEntity.ok().build(); // 200 OK
    }

    @Operation(summary = "하드웨어 삭제(폐기)", description = "특정 물리 하드웨어를 삭제(폐기) 처리합니다. 단, 해당 장비 위에 구동 중인 논리적 서버나 네트워크 장비가 있을 경우 삭제할 수 없습니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHardware(@PathVariable Long id) {
        log.info("Hardware API: 삭제 요청 - ID: {}", id);
        hardwareService.deleteHardware(id);
        return ResponseEntity.noContent().build(); // 204 No Content
    }

    @GetMapping("/excel/download")
    public void downloadExcel(@RequestParam Map<String, String> searchParams, HttpServletResponse response)
            throws Exception {
        // // 페이징 없이 최대 10,000건 조회
        // PageRequest maxPageRequest = PageRequest.of(0, 10000,
        // Sort.by("id").descending());
        // Page<HardwareResponse> hardwarePage =
        // hardwareService.searchHardwares(searchParams, maxPageRequest);

        // List<String> headers = Arrays.asList(
        // "위치", "랙 정보", "슬롯", "크기", "구분", "설명", "모델", "시리얼번호", "도입년도", "전원여부");

        // List<List<Object>> dataList = new ArrayList<>();
        // for (HardwareResponse hw : hardwarePage.getContent()) {
        // dataList.add(Arrays.asList(
        // hw.getLocationName() != null ? hw.getLocationName() : "-",
        // hw.getRackNo() != null ? hw.getRackNo() : "-",
        // hw.getRackPosition(),
        // hw.getSize(),
        // hw.getEquipmentType(),
        // hw.getDescription(),
        // hw.getModel(),
        // hw.getSerialNo(),
        // hw.getIntroductionYear(),
        // // 전원여부는 A/B 라인 상태를 요약해서 표시
        // hw.getIsSinglePower() ? hw.getPowerLine() : "O"));
        // }
        // ExcelUtil.download(response, "AKIMS_하드웨어_자산대장", headers, dataList);
        log.info("Hardware API: 엑셀 다운로드 요청");
        hardwareService.downloadExcel(searchParams, response);
    }

    // ==========================================
    // 2. 🌟 하드웨어 엑셀 업로드 (명칭 기반 ID 매핑)
    // ==========================================
    @PostMapping("/excel/upload")
    public ResponseEntity<Map<String, String>> uploadExcel(@RequestParam("file") MultipartFile file) {
        // try {
        // List<List<String>> excelData = ExcelUtil.readExcel(file);
        // int successCount = 0;

        // // 랙 전체 목록 캐싱 (텍스트 매핑용)
        // List<Rack> allRacks = rackRepository.findAll();

        // for (int i = 1; i < excelData.size(); i++) {
        // List<String> row = excelData.get(i);
        // if (row.size() < 7 || row.get(6).trim().isEmpty())
        // continue; // 시리얼번호(6번) 필수

        // try {
        // HardwareCreateRequest request = new HardwareCreateRequest();

        // // [1] 랙 정보 -> Rack ID 매핑 (위치와 랙번호 조합으로 찾음)
        // String rackNoStr = row.get(1).trim();
        // Rack matchedRack = allRacks.stream()
        // .filter(r -> r.getRackNo().equals(rackNoStr))
        // .findFirst().orElse(null);

        // if (matchedRack != null)
        // request.setRackId(matchedRack.getId());

        // request.setRackPosition(Integer.parseInt(row.get(2))); // [2] 슬롯
        // request.setSize(Integer.parseInt(row.get(3))); // [3] 크기
        // request.setEquipmentType(EquipmentType.valueOf(row.get(4).toUpperCase())); //
        // [4] 구분
        // request.setDescription(row.get(5)); // [5] 설명
        // request.setModel(row.get(6)); // [6] 모델
        // request.setSerialNo(row.get(7).trim()); // [7] 시리얼번호
        // request.setIntroductionYear(Integer.parseInt(row.get(8))); // [8] 도입년도

        // // [9] 전원여부 파싱 (예: "A:ON / B:OFF" 형태일 경우)
        // if (row.get(9).toUpperCase().equals("O")) {
        // request.setIsSinglePower(false);
        // } else {
        // request.setIsSinglePower(true);
        // request.setPowerLine(row.get(9).toUpperCase());
        // }

        // hardwareService.createHardware(request);
        // successCount++;
        // } catch (Exception e) {
        // // 개별 행 오류 시 로그 남기고 계속 진행
        // }
        // }
        // return ResponseEntity.ok(Map.of("message", "총 " + successCount + "건의 하드웨어가
        // 등록되었습니다."));
        // } catch (Exception e) {
        // return ResponseEntity.status(500).body(Map.of("message", "엑셀 처리 실패: " +
        // e.getMessage()));
        // }
        log.info("Hardware API: 엑셀 업로드 요청");
        try {
            int successCount = hardwareService.uploadExcel(file);
            return ResponseEntity.ok(Map.of("message", "총 " + successCount + "건의 하드웨어가 등록되었습니다."));
        } catch (Exception e) {
            log.error("엑셀 처리 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "엑셀 처리 실패: " + e.getMessage()));
        }
    }
}