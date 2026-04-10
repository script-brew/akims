package com.akplaza.infra.domain.device.controller;

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
import org.springframework.data.domain.Page;

import org.springframework.data.domain.Sort;

import com.akplaza.infra.domain.device.dto.ServerCreateRequest;
import com.akplaza.infra.domain.device.dto.ServerResponse;
import com.akplaza.infra.domain.device.dto.ServerUpdateRequest;

import com.akplaza.infra.domain.device.service.ServerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "6. Server API", description = "서버(물리/가상/클라우드) 인스턴스 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/servers")
@RequiredArgsConstructor
public class ServerController {

    private final ServerService serverService;

    @Operation(summary = "서버 등록", description = "새로운 서버 인스턴스를 등록합니다. IP 목록을 함께 전송하면 자동 할당됩니다.")
    @PostMapping
    public ResponseEntity<Long> createServer(@Valid @RequestBody ServerCreateRequest request) {
        log.info("Server API: 등록 요청 - HostName: {}", request.getHostName());
        Long serverId = serverService.createServer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(serverId); // 201 Created
    }

    @Operation(summary = "서버 단건 조회", description = "특정 서버의 상세 정보(하드웨어, IP 정보 포함)를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<ServerResponse> getServer(@PathVariable Long id) {
        log.debug("Server API: 단건 조회 요청 - ID: {}", id);
        ServerResponse response = serverService.getServer(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<ServerResponse>> getServers(
            @RequestParam Map<String, String> searchParams,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,desc") String sort) {

        searchParams.remove("page");
        searchParams.remove("size");
        searchParams.remove("sort");
        String[] sortArr = sort.split(",");
        Sort.Direction direction = sortArr.length > 1 && sortArr[1].equalsIgnoreCase("asc") ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortArr[0]));

        // PageRequest를 Service로 전달 (Service 메서드 파라미터가 맞는지 확인 필요)
        Page<ServerResponse> result = serverService.searchServers(searchParams, pageRequest);
        return ResponseEntity.ok(result);
    }

    // @Operation(summary = "서버 전체 조회", description = "시스템에 등록된 전체 서버 목록을 조회합니다.")
    // @GetMapping
    // public ResponseEntity<List<ServerResponse>> getAllServers() {
    // log.debug("Server API: 전체 목록 조회 요청");
    // List<ServerResponse> responses = serverService.getAllServers();
    // return ResponseEntity.ok(responses);
    // }

    @Operation(summary = "서버 정보 수정", description = "특정 서버의 정보(스펙 업그레이드, OS 변경 등)를 업데이트합니다.")
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateServer(
            @PathVariable Long id,
            @Valid @RequestBody ServerUpdateRequest request) {
        log.info("Server API: 수정 요청 - ID: {}", id);
        serverService.updateServer(id, request);
        return ResponseEntity.ok().build(); // 200 OK
    }

    @Operation(summary = "서버 삭제", description = "특정 서버를 삭제합니다. 삭제 시 할당되었던 IP는 자동으로 회수(초기화)됩니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteServer(@PathVariable Long id) {
        log.info("Server API: 삭제 요청 - ID: {}", id);
        serverService.deleteServer(id);
        return ResponseEntity.noContent().build(); // 204 No Content
    }

    @Operation(summary = "서버 목록 엑셀 다운로드", description = "검색 조건에 맞는 서버 목록을 엑셀 파일로 다운로드합니다.")
    @GetMapping("/excel/download")
    public void downloadExcel(@RequestParam Map<String, String> searchParams,
            @RequestParam(defaultValue = "id,desc") String sort, HttpServletResponse response)
            throws Exception {
        log.info("Server API: 엑셀 다운로드 요청");
        serverService.downloadExcel(searchParams, sort, response);

    }

    // ==========================================
    // 2. 🌟 대망의 업로드 (역 파싱 로직)
    // ==========================================
    @Operation(summary = "서버 대량 업로드")
    @PostMapping("/excel/upload")
    public ResponseEntity<Map<String, String>> uploadExcel(@RequestParam("file") MultipartFile file) {
        // try {
        // List<List<String>> excelData = ExcelUtil.readExcel(file);
        // int successCount = 0;

        // // 텍스트 매핑을 위한 기초 데이터 풀 캐싱 (DB 부하 방지)
        // List<Hardware> allHardware = hardwareRepository.findAll();
        // List<IpCidr> allCidrs = ipCidrRepository.findAll();

        // // i=1 (헤더인 0번 인덱스는 건너뜀)
        // for (int i = 1; i < excelData.size(); i++) {
        // List<String> row = excelData.get(i);
        // if (row.size() < 5 || row.get(4).trim().isEmpty())
        // continue; // 서버명(4번열)이 없으면 무시

        // try {
        // // [4] 서버명
        // String hostName = row.get(4).trim();
        // // [1] 분류 (예: WEB, DB)
        // ServerCategory category = ServerCategory.valueOf(row.get(1).toUpperCase());
        // // [3] 용도 (예: PRD, DEV)
        // Environment env = Environment.valueOf(row.get(3).toUpperCase());

        // // [2] H/W 시리얼 -> Hardware ID 매핑
        // String serialNo = row.get(2).trim();
        // Long hardwareId = null;
        // ServerType serverType = ServerType.VIRTUAL;
        // if (!serialNo.equals("-") && !serialNo.isEmpty()) {
        // Hardware hw = allHardware.stream().filter(h ->
        // h.getSerialNo().equals(serialNo)).findFirst()
        // .orElse(null);
        // if (hw != null) {
        // hardwareId = hw.getId();
        // serverType = ServerType.PHYSICAL;
        // }
        // }

        // // [6] OS, [5] 설명
        // String os = row.get(6).trim();
        // String desc = row.get(5).trim();

        // // [8] CPU, [9] Mem
        // double cpu = row.get(8).isEmpty() ? 1.0 : Double.parseDouble(row.get(8));
        // double mem = row.get(9).isEmpty() ? 1.0 : Double.parseDouble(row.get(9));

        // // [13] HA, [12] 백업, [14] 모니터링
        // boolean isHa = row.get(13).equalsIgnoreCase("O");
        // // String backupStr = row.get(12).isEmpty() ? "NO_BACKUP" : row.get(12);
        // ServerBackup backupStr = ServerBackup.valueOf(row.get(12).isEmpty() ?
        // "NO_BACKUP" : row.get(12));
        // ServerMonitoring monStr = ServerMonitoring.valueOf(
        // row.get(12).isEmpty() ? "NO_MONITORING" : row.get(12));

        // // 🚨 1:N 다중 정보 역 파싱 (개행 분리)

        // // [7] IP 매핑: 10.10.10.5 -> 속해있는 CIDR 대역 ID를 역추적
        // List<IpAssignRequest> ipReqs = new ArrayList<>();
        // if (!row.get(7).isEmpty()) {
        // for (String ip : row.get(7).split("\n")) {
        // ip = ip.trim();
        // if (ip.isEmpty() || ip.equals("-"))
        // continue;
        // Long cidrId = findMatchingCidrId(ip, allCidrs);
        // if (cidrId != null)
        // ipReqs.add(new IpAssignRequest(cidrId, ip));
        // }
        // }

        // // [10] Disk 매핑: SSD(100GB, /) -> 타입, 사이즈, 마운트 분리
        // List<DiskRequest> diskReqs = new ArrayList<>();
        // if (!row.get(10).isEmpty()) {
        // for (String d : row.get(10).split("\n")) {
        // d = d.trim();
        // if (d.isEmpty() || d.equals("-"))
        // continue;
        // DiskType type = DiskType.valueOf(d.substring(0, d.indexOf("(")).trim());
        // String inner = d.substring(d.indexOf("(") + 1, d.indexOf(")")); // "100GB, /"
        // String[] parts = inner.split(",");
        // int size = Integer.parseInt(parts[0].replace("GB", "").trim());
        // String mount = parts.length > 1 ? parts[1].trim() : "/";

        // diskReqs.add(new DiskRequest(type, size, mount));
        // }
        // }

        // // [11] Software 매핑: Tomcat 8.5 -> 이름, 버전 분리
        // List<SoftwareRequest> swReqs = new ArrayList<>();
        // if (!row.get(11).isEmpty()) {
        // for (String s : row.get(11).split("\n")) {
        // s = s.trim();
        // if (s.isEmpty() || s.equals("-"))
        // continue;
        // int lastSpace = s.lastIndexOf(" ");
        // String name = lastSpace > 0 ? s.substring(0, lastSpace).trim() : s;
        // String version = lastSpace > 0 ? s.substring(lastSpace + 1).trim() : "";

        // swReqs.add(new SoftwareRequest(name, version, "", ""));
        // }
        // }

        // // 🌟 DTO 조립 및 저장
        // ServerCreateRequest request = new ServerCreateRequest();
        // request.setHostName(hostName);
        // request.setServerCategory(category);
        // request.setEnvironment(env);
        // request.setServerType(serverType);
        // request.setOs(os);
        // request.setCpuCore(cpu);
        // request.setMemoryGb(mem);
        // request.setHardwareId(hardwareId);
        // request.setDescription(desc);
        // request.setHa(isHa);
        // request.setBackupInfo(backupStr);
        // request.setMonitoringInfo(monStr);
        // request.setIps(ipReqs);
        // request.setDisks(diskReqs);
        // request.setSoftwares(swReqs);

        // serverService.createServer(request);
        // successCount++;
        // } catch (Exception e) {
        // log.error("엑셀 {}번째 행({}) 파싱 실패: {}", i + 1, row.get(4), e.getMessage());
        // // 특정 행 에러 시 무시하고 다음 행 진행
        // }
        // }
        // return ResponseEntity.ok(Map.of("message", "총 " + successCount + "건의 서버가 엑셀로
        // 일괄 등록되었습니다."));
        // } catch (Exception e) {
        // return ResponseEntity.status(500).body(Map.of("message", "엑셀 처리 실패: " +
        // e.getMessage()));
        // }

        log.info("Server API: 엑셀 업로드 요청");
        try {
            int successCount = serverService.uploadExcel(file);
            return ResponseEntity.ok(Map.of("message", "총 " + successCount + "건의 서버가 일괄 등록되었습니다."));
        } catch (Exception e) {
            log.error("엑셀 처리 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "엑셀 처리 실패: " + e.getMessage()));
        }
    }
}