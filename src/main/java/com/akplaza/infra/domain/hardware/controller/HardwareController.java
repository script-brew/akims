package com.akplaza.infra.domain.hardware.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

import com.akplaza.infra.domain.device.dto.ServerResponse;
import com.akplaza.infra.domain.hardware.dto.HardwareCreateRequest;
import com.akplaza.infra.domain.hardware.dto.HardwareResponse;
import com.akplaza.infra.domain.hardware.dto.HardwareUpdateRequest;
import com.akplaza.infra.domain.hardware.service.HardwareService;
import com.akplaza.infra.domain.network.dto.IpAssignRequest;
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

    // @Operation(summary = "하드웨어 목록 엑셀 다운로드", description = "검색 조건에 맞는 하드웨어 목록을 엑셀
    // 파일로 다운로드합니다.")
    // @GetMapping("/excel/download")
    // public void downloadExcel(@RequestParam Map<String, String> searchParams,
    // HttpServletResponse response)
    // throws Exception {

    // // 1. 현재 검색 조건에 맞는 데이터를 모두 가져옵니다
    // PageRequest maxPageRequest = PageRequest.of(0, 10000,
    // Sort.by("id").descending());
    // Page<HardwareResponse> serverPage =
    // hardwareService.searchHardwares(searchParams, maxPageRequest);

    // // 2. 🌟 요청하신 실무형 커스텀 엑셀 헤더 적용
    // List<String> headers = Arrays.asList(
    // "위치", "분류", "H/W", "용도", "서버명", "설명", "운영체제(Version)",
    // "IP", "CPU", "Memory", "Disk", "Softwares", "백업", "HA", "모니터링");

    // // 3. 데이터를 2차원 리스트로 변환
    // List<List<Object>> dataList = new ArrayList<>();
    // for (HardwareResponse srv : serverPage.getContent()) {

    // // 🌟 핵심 방어 로직: 1:N List 객체들을 엑셀 한 칸에 들어갈 수 있도록 예쁜 문자열로 변환 (예: "10.0.0.1,
    // // 10.0.0.2")
    // String ipStr = srv.getIps() != null
    // ?
    // srv.getIps().stream().map(IpAssignRequest::getIpAddress).collect(Collectors.joining("\n"))
    // : "";

    // String diskStr = srv.getDisks() != null ? srv.getDisks().stream()
    // .map(d -> d.getDiskType() + "(" + d.getSize() + "GB, " + d.getMountPoint() +
    // ")")
    // .collect(Collectors.joining("\n")) : "";

    // String swStr = srv.getSoftwares() != null ? srv.getSoftwares().stream()
    // .map(s -> s.getName() + " " + (s.getVersion() != null ? s.getVersion() : ""))
    // .collect(Collectors.joining("\n")) : "";

    // dataList.add(Arrays.asList(
    // srv.getLocationName() != null ? srv.getLocationName() : "-",
    // srv.getServerCategory(),
    // srv.getSerialNo() != null ? srv.getSerialNo() : "-",
    // srv.getEnvironment(),
    // srv.getHostName(),
    // srv.getDescription() != null ? srv.getDescription() : "",
    // srv.getOs(),
    // ipStr, // 파싱된 IP 문자열
    // srv.getCpuCore(),
    // srv.getMemoryGb(),
    // diskStr, // 파싱된 Disk 문자열
    // swStr, // 파싱된 S/W 문자열
    // srv.getBackupInfo(),
    // srv.isHa() ? "O" : "X", // 롬복의 boolean getter는 기본적으로 isHa()로 생성됩니다.
    // srv.getMonitoringInfo()));
    // }

    // // 4. 유틸리티를 통한 즉시 다운로드 (파일명도 직관적으로 변경)
    // ExcelUtil.download(response, "AKIMS_서버대장_추출", headers, dataList);
    // }

    // @Operation(summary = "서버 대량 업로드", description = "엑셀 파일을 업로드하여 서버를 대량으로
    // 등록합니다.")
    // @PostMapping("/excel/upload")
    // public ResponseEntity<String> uploadExcel(@RequestParam("file") MultipartFile
    // file) {
    // try {
    // List<List<String>> excelData = ExcelUtil.readExcel(file);

    // // TODO: excelData를 파싱하여 ServerCreateRequest로 변환하고
    // serverService.createServer()
    // // 호출 로직 작성
    // // (첫 번째 줄은 헤더이므로 건너뛰고 1번 인덱스부터 읽음)
    // log.info("읽어들인 엑셀 총 행(Row) 수: {}", excelData.size());

    // return ResponseEntity.ok("엑셀 업로드 및 처리 완료 (총 " + (excelData.size() - 1) +
    // "건)");
    // } catch (Exception e) {
    // log.error("엑셀 업로드 실패", e);
    // return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("엑셀 처리 중
    // 오류 발생: " + e.getMessage());
    // }
    // }
}