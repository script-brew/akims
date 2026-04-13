import { api } from "../common/api.js";
import { ui } from "../common/ui.js";
import { SearchFilter } from "../common/search-filter.js";

// --- 1. DOM Elements Mapping ---
const serverGridContainer = document.getElementById("server-grid-container");

// Header Actions
const btnUploadExcel = document.getElementById("btn-upload-excel");
const excelFileInput = document.getElementById("excel-file-input");
const btnDownloadExcel = document.getElementById("btn-download-excel");
const btnOpenModal = document.getElementById("btn-open-modal");
const btnEdit = document.getElementById("btn-edit");
const btnDelete = document.getElementById("btn-delete");

// Detail View (Inline)
const serverDetailContainer = document.getElementById(
  "server-detail-container"
);
const detailContent = document.getElementById("server-detail-content");
const detailTitle = document.getElementById("detail-title");
const btnCloseDetail = document.getElementById("btn-close-detail");

// Edit Modal
const modalEdit = document.getElementById("srv-modal");
const btnCancel = document.getElementById("btn-cancel");
const btnSave = document.getElementById("btn-save");

// Modal Form Inputs
const inputId = document.getElementById("srv-id");
const selCategory = document.getElementById("srv-category");
const selEnvironment = document.getElementById("srv-environment");
const selType = document.getElementById("srv-type");
const inputHostName = document.getElementById("srv-hostname");
const inputOs = document.getElementById("srv-os");
const inputCpu = document.getElementById("srv-cpu");
const inputRam = document.getElementById("srv-ram");
const chkHa = document.getElementById("srv-ha");
const selMonitoring = document.getElementById("srv-monitoring");
const selBackup = document.getElementById("srv-backup");
const selHardware = document.getElementById("srv-hardware");
const inputDesc = document.getElementById("srv-desc");

// Dynamic Lists Containers & Buttons
const ipListContainer = document.getElementById("ip-list-container");
const btnAddIp = document.getElementById("btn-add-ip");
const diskListContainer = document.getElementById("disk-list-container");
const btnAddDisk = document.getElementById("btn-add-disk");
const swListContainer = document.getElementById("sw-list-container");
const hardwareMappingArea = document.getElementById("hardware-mapping-area");
const btnAddSw = document.getElementById("btn-add-sw");

// --- 2. State ---
let hardwareList = [];
let cidrList = [];
let serverGrid;
let currentSortParam = "id,desc";
let currentKeyword = "";
let activeFilters = [];
let searchFilter; // 🌟 필터 인스턴스 변수

// ==========================================
// 🚀 초기화
// ==========================================
document.addEventListener("DOMContentLoaded", async () => {
  // 🌟 서버 화면에 맞는 옵션을 주입하여 검색 필터 초기화!
  const filterOptions = [
    { value: "hostName", label: "서버명 (Host)" },
    { value: "os", label: "운영체제 (OS)" },
    { value: "description", label: "설명/비고" },
  ];

  // (컨테이너 요소, 주입할 옵션배열, 검색 실행 콜백함수)
  searchFilter = new SearchFilter(
    document.getElementById("server-search-filter"),
    filterOptions,
    () => {
      serverGrid.forceRender();
    }
  );

  loadHardwares();
  loadCidrs();
  initGrid();
  setupEventListeners();
});

async function loadHardwares() {
  try {
    const res = await api.get("/api/v1/hardwares?size=1000");
    hardwareList = res.content || [];
    if (selHardware) {
      selHardware.innerHTML =
        `<option value="">-- 매핑 안 함 (가상서버인 경우 생략) --</option>` +
        hardwareList
          .map(
            (h) =>
              `<option value="${h.id}">[${h.model}] ${h.serialNo} (이름: ${h.description})</option>`
          )
          .join("");
    }
  } catch (e) {
    console.error("하드웨어 로드 실패", e);
  }
}

async function loadCidrs() {
  try {
    const res = await api.get("/api/v1/ip-cidrs?size=1000");
    cidrList = res.content || [];
  } catch (e) {
    console.error("CIDR 로드 실패", e);
  }
}

// 🌟 prev가 undefined일 때를 대비한 안전망 함수 (버그 해결 핵심)
function getSafeUrl(prev) {
  if (typeof prev !== "undefined") return prev;
  let filterParams =
    typeof searchFilter !== "undefined" ? searchFilter.getQueryParams() : "";
  if (filterParams === "?") filterParams = "";
  return `/api/v1/servers${filterParams}`;
}

// ==========================================
// 🚀 Grid.js 초기화
// ==========================================
function initGrid() {
  serverGrid = new gridjs.Grid({
    columns: [
      { id: "id", name: "ID", hidden: true },
      {
        id: "checkbox",
        name: gridjs.html(
          '<input type="checkbox" id="check-all" title="전체선택">'
        ),
        sort: false,
        width: "60px",
        formatter: (cell, row) =>
          gridjs.html(
            `<input type="checkbox" class="data-checkbox" value="${row.cells[0].data}">`
          ),
      },
      { id: "environment", name: "환경", width: "100px" },
      { id: "serverType", name: "유형", width: "100px" },
      { id: "serverCategory", name: "분류", width: "120px" },
      {
        id: "hostName",
        name: "서버명",
        width: "250px",
        formatter: (cell, row) =>
          gridjs.html(
            `<a href="javascript:void(0);" onclick="window.viewServerDetail(${
              row.cells[0].data
            })" style="color:var(--primary-color); font-weight:bold; text-decoration:underline;">${ui.escapeHtml(
              cell
            )}</a>`
          ),
      },
      { id: "os", name: "OS", width: "180px" },
      {
        id: "cpu",
        name: "CPU",
        width: "150px",
      },
      {
        id: "memory",
        name: "Memory",
        width: "150px",
      },
      {
        id: "ips",
        name: "IP 주소",
        sort: false,
        width: "200px",
        // 🌟 여러 IP를 뱃지 형태로 나열
        formatter: (cell) => {
          if (!cell || cell.length === 0)
            return gridjs.html('<span style="color:#ccc;">-</span>');
          const badges = cell
            .map(
              (ip) =>
                `<code style="background:#eaf2f8; color:#2980b9; padding:2px 5px; border-radius:3px; font-size:0.8rem; margin-right:3px;">${ip.ipAddress}</code>`
            )
            .join("");
          return gridjs.html(
            `<div style="display:flex; flex-wrap:wrap; gap:2px;">${badges}</div>`
          );
        },
      },
      {
        id: "hardware",
        name: "연결 하드웨어",
        sort: false,
        width: "250px",
        // 🌟 hardwareId를 이용해 hardwareList에서 모델 정보 매핑
        formatter: (cell) => {
          if (!cell)
            return gridjs.html('<span style="color:#ccc;">미매핑</span>');
          const hw = hardwareList.find((h) => h.id === cell);
          if (!hw)
            return gridjs.html(
              '<span style="color:#e67e22;">알 수 없음</span>'
            );
          return gridjs.html(
            `<div title="S/N: ${hw.serialNo}" style="font-size:0.85rem;"><b>${hw.description}</b><br/><small style="color:#7f8c8d;">${hw.serialNo}</small></div>`
          );
        },
      },
    ],
    // 🌟 Grid.js 공식 Server-Side 파이프라인 (데이터 로드 주체)
    server: {
      url: "/api/v1/servers",
      headers: {
        Authorization: `Bearer ${localStorage.getItem("accessToken") || ""}`,
        "Content-Type": "application/json",
      },
      then: (data) =>
        data.content.map((srv) => [
          srv.id,
          null,
          srv.environment,
          srv.serverType,
          srv.serverCategory,
          srv.hostName,
          srv.os,
          srv.cpuCore,
          srv.memoryGb,
          srv.ips, // IP 목록 (Array)
          srv.hardwareId, // 연결된 하드웨어 ID
        ]),
      total: (data) => data.totalElements,
    },
    search: false,
    // 🌟 파이프라인 2. 정렬 (URL 조립)
    sort: {
      multiColumn: false,
      server: {
        url: (prev, columns) => {
          let sortString = "id,desc"; // 기본 정렬
          if (columns && columns.length > 0) {
            const col = columns[0];
            const dir = col.direction === 1 ? "asc" : "desc";
            const colIds = [
              "id",
              "checkbox",
              "environment",
              "serverType",
              "serverCategory",
              "hostName",
              "os",
              "cpu",
              "memory",
              "ips",
              "hardware",
            ];
            sortString = `${colIds[col.index]},${dir}`;
          }
          currentSortParam = sortString; // 엑셀 다운로드를 위해 저장
          return `${prev}?sort=${sortString}`;
        },
      },
    },
    // 🌟 파이프라인 3. 페이징 (URL 조립 - 이전 버그 완벽 해결 구간)
    pagination: {
      enabled: true,
      limit: 20,
      server: {
        url: (prev, page, limit) => {
          // 1) searchFilter에서 '?environment=PRD' 형태로 가져옴
          let filterParams =
            typeof searchFilter !== "undefined"
              ? searchFilter.getQueryParams()
              : "";

          // 2) 이미 prev에 '?sort='가 붙어있으므로, 앞의 '?'를 '&'로 바꿔서 예쁘게 이어붙임
          if (filterParams.startsWith("?")) {
            filterParams = "&" + filterParams.substring(1);
          }
          if (filterParams === "&") filterParams = "";

          // 3) 최종 URL 완성! (ex: /api/v1/servers?sort=id,desc&page=0&size=20&environment=PRD)
          return `${prev}&page=${page}&size=${limit}${filterParams}`;
        },
      },
    },
    // 🌟 테이블 UI 강력 확장 옵션 🌟
    fixedHeader: true, // 1) 헤더 고정
    height: "500px", // 1-1) 고정 헤더를 위한 컨테이너 높이 지정
    resizable: true, // 2) 컬럼 너비 드래그 조절
    style: {
      table: {
        "white-space": "nowrap", // 3) 셀 줄바꿈 방지
        "min-width": "600px", // 3-1) 화면이 작아도 1300px 유지 -> 가로 스크롤(Wide Table) 생성
      },
    },
    className: { table: "akims-custom-table" },
    language: {
      pagination: {
        previous: "이전",
        next: "다음",
        showing: "표시 중",
        results: "결과",
      },
      noRecordsFound: "데이터가 존재하지 않습니다.",
    },
  }).render(serverGridContainer);
}

// ==========================================
// 🚀 이벤트 리스너
// ==========================================
function setupEventListeners() {
  // 🌟 검색 필터의 [검색] 버튼 클릭 이벤트 연동 (Grid.js 강제 렌더링)
  // const btnCustomSearch = document.getElementById("btn-search");
  // if (btnCustomSearch) {
  //   btnCustomSearch.addEventListener("click", () => {
  //     // 1페이지부터 다시 조회
  //     serverGrid.updateConfig({ pagination: { page: 0 } }).forceRender();
  //   });
  // }
  // 모달 제어
  btnOpenModal.addEventListener("click", openCreateModal);
  btnCancel.addEventListener("click", () => {
    modalEdit.style.display = "none";
  });
  btnCloseDetail.addEventListener("click", () => {
    serverDetailContainer.style.display = "none";
  });
  btnSave.addEventListener("click", saveServer);

  // 상단 액션 버튼
  btnEdit.addEventListener("click", handleEditAction);
  btnDelete.addEventListener("click", handleDeleteAction);

  // 동적 추가 버튼
  btnAddIp.addEventListener("click", () => createIpRow());
  btnAddDisk.addEventListener("click", () => createDiskRow());
  btnAddSw.addEventListener("click", () => createSwRow());
  selType.addEventListener("change", (e) => {
    if (e.target.value === "AWS_CLOUD" || e.target.value === "SCP_CLOUD") {
      hardwareMappingArea.style.display = "none"; // 클라우드는 물리 장비 선택 안 함
      selHardware.value = "";
    } else {
      hardwareMappingArea.style.display = "block"; // 물리/VM은 물리 장비 선택 창 노출
    }
  });

  // Grid 체크박스 위임
  serverGridContainer.addEventListener("change", (e) => {
    if (e.target.id === "check-all") {
      const isChecked = e.target.checked;
      document
        .querySelectorAll(".data-checkbox")
        .forEach((cb) => (cb.checked = isChecked));
    }
  });

  // 엑셀 업로드 (버튼 클릭 -> file input 클릭 트리거)
  btnUploadExcel.addEventListener("click", () => excelFileInput.click());
  excelFileInput.addEventListener("change", async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    const formData = new FormData();
    formData.append("file", file);

    try {
      const token = localStorage.getItem("accessToken");
      const response = await fetch("/api/v1/servers/excel/upload", {
        method: "POST",
        headers: { Authorization: `Bearer ${token}` },
        body: formData,
      });
      if (!response.ok) throw new Error(await response.text());
      ui.showAlert("엑셀 업로드가 완료되었습니다.");
      excelFileInput.value = "";
      serverGrid.forceRender();
    } catch (err) {
      ui.showAlert("업로드 실패: " + err.message);
    }
  });

  // 엑셀 다운로드
  btnDownloadExcel.addEventListener("click", () => {
    let url = `/api/v1/servers/excel/download?sort=${currentSortParam}`;
    if (currentKeyword) url += `&keyword=${encodeURIComponent(currentKeyword)}`;
    window.location.href = url;
  });
}

// ==========================================
// 🚀 선택 작업 (수정/삭제)
// ==========================================
function handleEditAction() {
  const checked = Array.from(
    document.querySelectorAll(".data-checkbox:checked")
  );
  if (checked.length !== 1) {
    ui.showAlert("수정할 서버를 1개만 선택해주세요.");
    return;
  }
  openEditModal(checked[0].value);
}

async function handleDeleteAction() {
  const checked = Array.from(
    document.querySelectorAll(".data-checkbox:checked")
  );
  if (checked.length === 0) {
    ui.showAlert("삭제할 서버를 선택해주세요.");
    return;
  }
  if (confirm(`선택한 ${checked.length}개의 서버를 삭제하시겠습니까?`)) {
    try {
      for (const cb of checked) {
        await api.delete(`/api/v1/servers/${cb.value}`);
      }
      ui.showAlert("삭제되었습니다.");
      serverGrid.forceRender();
    } catch (e) {
      ui.showAlert("삭제 중 오류가 발생했습니다.");
    }
  }
}

window.deleteServer = async (id) => {
  if (confirm("정말로 이 서버를 삭제하시겠습니까?")) {
    try {
      await api.delete(`/api/v1/servers/${id}`);
      ui.showAlert("삭제되었습니다.");
      serverGrid.forceRender();
    } catch (e) {
      ui.showAlert("삭제 실패: " + e.message);
    }
  }
};

// ==========================================
// 🚀 모달 (생성/수정/상세)
// ==========================================
function openCreateModal() {
  inputId.value = "";
  selCategory.value = "WEB";
  inputHostName.value = "";
  selEnvironment.value = "PRD";
  selType.value = "VIRTUAL";
  inputOs.value = "";
  inputCpu.value = 1;
  inputRam.value = 0.5;
  selHardware.value = "";
  inputDesc.value = "";
  chkHa.checked = false;
  selBackup.value = "NO_BACKUP";
  selMonitoring.value = "NO_MONITORING";

  ipListContainer.innerHTML = "";
  diskListContainer.innerHTML = "";
  swListContainer.innerHTML = "";

  createIpRow();
  createDiskRow("SSD", 100, "OS 영역");

  hardwareMappingArea.style.display = "block";
  document.getElementById("modal-title").innerText = "새 서버 등록";
  modalEdit.style.display = "block";
}

async function openEditModal(id) {
  ipListContainer.innerHTML = "";
  diskListContainer.innerHTML = "";
  swListContainer.innerHTML = "";

  try {
    const t = await api.get(`/api/v1/servers/${id}`);
    inputId.value = t.id;
    selHardware.value = t.hardwareId || "";
    inputHostName.value = t.hostName || "";
    selCategory.value = t.serverCategory || "WEB";
    selEnvironment.value = t.environment || "PRD";
    selType.value = t.serverType || "VIRTUAL";
    inputOs.value = t.os || "";
    inputDesc.value = t.description || "";
    inputCpu.value = t.cpuCore || 1;
    inputRam.value = t.memoryGb || 1;
    chkHa.checked = t.ha || false;
    selBackup.value = t.backupInfo || "NO_BACKUP";
    selMonitoring.value = t.monitoringInfo || "NO_MONITORING";

    if (t.ips && t.ips.length > 0)
      t.ips.forEach((ip) => createIpRow(ip.cidrId, ip.ipAddress));
    else createIpRow();

    if (t.disks && t.disks.length > 0)
      t.disks.forEach((d) => createDiskRow(d.diskType, d.size, d.diskName));
    else createDiskRow();

    if (t.softwares && t.softwares.length > 0)
      t.softwares.forEach((sw) => createSwRow(sw.name, sw.version));

    hardwareMappingArea.style.display =
      t.serverType === "AWS_CLOUD" || t.serverType === "SCP_CLOUD"
        ? "none"
        : "block";

    ui.openModal("srv-modal", "modal-title", "서버 정보 수정");
  } catch (e) {
    ui.showAlert("정보 로딩 실패");
  }
}

async function saveServer() {
  // 연관 데이터(IP, Disk, SW) 추출
  const ips = [];
  document.querySelectorAll(".ip-row").forEach((row) => {
    const cidrId = row.querySelector(".ip-cidr-sel").value;
    const ipAddr = row.querySelector(".ip-addr-input").value.trim();
    if (ipAddr)
      ips.push({ cidrId: cidrId ? parseInt(cidrId) : null, ipAddress: ipAddr });
  });

  const disks = [];
  document.querySelectorAll(".disk-row").forEach((row) => {
    const dType = row.querySelector(".disk-type-sel").value;
    const dCap = row.querySelector(".disk-cap-input").value;
    const dName = row.querySelector(".disk-name-input").value.trim();
    if (dType && dCap)
      disks.push({ diskType: dType, size: parseInt(dCap), diskName: dName });
  });

  const softwares = [];
  document.querySelectorAll(".sw-row").forEach((row) => {
    const sName = row.querySelector(".sw-name-input").value.trim();
    const sVer = row.querySelector(".sw-ver-input").value.trim();
    if (sName) softwares.push({ name: sName, version: sVer });
  });

  const payload = {
    hardwareId: selHardware.value || null,
    hostName: inputHostName.value.trim(),
    serverCategory: selCategory.value,
    environment: selEnvironment.value,
    serverType: selType.value,
    os: inputOs.value.trim(),
    description: inputDesc.value.trim(),
    cpuCore: parseFloat(inputCpu.value),
    memoryGb: parseFloat(inputRam.value),
    isHa: chkHa.checked,
    backupInfo: selBackup.value,
    monitoringInfo: selMonitoring.value,
    ips: ips,
    disks: disks,
    softwares: softwares,
  };

  if (!payload.hostName) return ui.showAlert("서버명(HostName)은 필수입니다.");

  try {
    const id = inputId.value;
    if (id) {
      await api.put(`/api/v1/servers/${id}`, payload);
      ui.showAlert("수정되었습니다.");
    } else {
      await api.post("/api/v1/servers", payload);
      ui.showAlert("등록되었습니다.");
    }
    modalEdit.style.display = "none";
    serverGrid.forceRender();
  } catch (error) {
    ui.showAlert("저장 실패: " + error.message);
  }
}

window.viewServerDetail = async (id) => {
  try {
    const srv = await api.get(`/api/v1/servers/${id}`);

    // 1. 타이틀 설정
    const envBadge =
      srv.environment === "PRD"
        ? '<span style="color:#c0392b;">[운영]</span>'
        : '<span style="color:#27ae60;">[개발/검증]</span>';
    detailTitle.innerHTML = `${envBadge} ${srv.hostName} / ${srv.description} 상세 자산 정보`;

    // 2. 하드웨어/기본 정보 파싱
    const hwInfo = srv.hardwareId
      ? `
    <div style="margin-bottom: 8px;"><strong>위치: </strong> ${srv.locationName}</div>
    <div style="margin-bottom: 8px;"><strong>랙정보: </strong> ${srv.rackNo}</div>
    <div style="margin-bottom: 8px;"><strong>하드웨어 정보: </strong> ${srv.hardwareDescription}</div>
    <div style="margin-bottom: 8px;"><strong>모델: </strong> ${srv.hardwareModel}</div>
    <div style="margin-bottom: 8px;"><strong>시리얼넘버: </strong> ${srv.serialNo}</div>
    `
      : '<span style="color:#999;">클라우드 / 가상화 (미할당)</span>';

    // 3. 디스크 정보 파싱
    const disks =
      srv.disks && srv.disks.length > 0
        ? srv.disks
            .map(
              (d) =>
                `<div style="margin-bottom:4px;">[${d.diskType}] <strong>${d.size}GB</strong> <code style="font-size:0.8rem;">${d.diskName}</code></div>`
            )
            .join("")
        : '<span style="color:#999;">등록된 디스크 없음</span>';

    // 4. 소프트웨어 및 DBMS 정보 파싱
    const sw =
      srv.softwares && srv.softwares.length > 0
        ? srv.softwares
            .map(
              (s) =>
                `<div>• ${s.name} <small style="color:#777;">(${
                  s.version || "v.알수없음"
                })</small></div>`
            )
            .join("")
        : '<span style="color:#999;">등록된 S/W 없음</span>';

    const dbms =
      srv.dbmses && srv.dbmses.length > 0
        ? srv.dbmses
            .map(
              (d) =>
                `<div>• <strong style="color:#8e44ad;">${
                  d.name
                }</strong> <small style="color:#777;">(${
                  d.version || ""
                })</small></div>`
            )
            .join("")
        : '<span style="color:#999;">등록된 DBMS 없음</span>';

    // 5. 4분할 카드 렌더링
    detailContent.innerHTML = `
        <div style="background: #f8f9fa; padding: 15px; border-radius: 6px; border: 1px solid #e0e0e0;">
            <h4 style="border-bottom: 2px solid #bdc3c7; padding-bottom: 8px; margin-bottom: 12px; color: #2c3e50;">기본 스펙</h4>
            <div style="margin-bottom: 8px;"><strong>OS:</strong> <span class="os-badge ${
              srv.os
            }">${srv.os}</span></div>
            <div style="margin-bottom: 8px;"><strong>CPU/RAM:</strong> ${
              srv.cpuCore
            } Core / ${srv.memoryGb} GB</div>
            <div style="margin-bottom: 8px;"><strong>유형:</strong> <span class="badge ${
              srv.serverType
            }">${srv.serverType}</span></div>
            <div style="margin-bottom: 8px;"><strong>HA 구성:</strong> ${
              srv.ha
                ? '<span style="color:#e74c3c; font-weight:bold;">Active (이중화)</span>'
                : "단일 노드"
            }</div>
            <div style="margin-bottom: 8px;"><strong>백업: </strong> ${
              srv.backupInfo === "NO_BACKUP"
                ? '<span style="color:#e74c3c; font-weight:bold;">X</span>'
                : srv.backupInfo
            }</div>
            <div style="margin-bottom: 8px;"><strong>모니터링: </strong> ${
              srv.monitoringInfo === "NO_MONITORING"
                ? '<span style="color:#e74c3c; font-weight:bold;">X</span>'
                : srv.monitoringInfo
            }</div>
        </div>

        <div style="background: #f8f9fa; padding: 15px; border-radius: 6px; border: 1px solid #e0e0e0;">
            <h4 style="border-bottom: 2px solid #bdc3c7; padding-bottom: 8px; margin-bottom: 12px; color: #2c3e50;">물리 장비 스펙</h4>
            <div style="margin-top:5px; line-height:1.4;">${hwInfo}</div>
        </div>

        <div style="background: #f8f9fa; padding: 15px; border-radius: 6px; border: 1px solid #e0e0e0;">
            <h4 style="border-bottom: 2px solid #bdc3c7; padding-bottom: 8px; margin-bottom: 12px; color: #2c3e50;">네트워크 & 스토리지</h4>
            <div style="margin-bottom: 8px;">
                <strong>할당된 IP 주소:</strong>
                <div style="margin-top:5px;">
                    ${
                      srv.ips && srv.ips.length > 0
                        ? srv.ips
                            .map(
                              (ip) =>
                                `<code style="display:inline-block; margin-bottom:4px; font-weight:bold;">${ip.ipAddress}</code>`
                            )
                            .join("<br>")
                        : '<span style="color:#999;">미할당</span>'
                    }
                </div>
            </div>
            <div style="margin-top: 15px; padding-top: 10px; border-top: 1px dashed #ccc;">
                <strong>스토리지 (디스크):</strong>
                <div style="margin-top:5px; line-height:1.4;">${disks}</div>
            </div>
        </div>

        <div style="background: #f8f9fa; padding: 15px; border-radius: 6px; border: 1px solid #e0e0e0;">
            <h4 style="border-bottom: 2px solid #bdc3c7; padding-bottom: 8px; margin-bottom: 12px; color: #2c3e50;">소프트웨어 (S/W)</h4>
            <div style="line-height:1.6; font-size:0.95rem;">
                ${sw}
            </div>
        </div>
    `;

    // 6. 컨테이너 노출 및 스크롤 이동
    serverDetailContainer.style.display = "block";
    serverDetailContainer.scrollIntoView({
      behavior: "smooth",
      block: "start",
    });
  } catch (error) {
    ui.showAlert("상세 로딩 실패");
  }
};

// ==========================================
// 🚀 동적 추가 행 렌더러 (IP, Disk, SW)
// ==========================================
function createIpRow(cidrId = "", ipAddress = "") {
  const row = document.createElement("div");
  row.className = "ip-row form-row";
  row.style.cssText = "margin-bottom:0; display:flex; gap:10px;";
  const cidrOptions = cidrList
    .map(
      (c) =>
        `<option value="${c.id}" ${c.id == cidrId ? "selected" : ""}>${
          c.cidrBlock
        } (${c.description || ""})</option>`
    )
    .join("");
  row.innerHTML = `
    <select class="ip-cidr-sel form-control" style="flex:1;"><option value="">-- 대역 선택 --</option>${cidrOptions}</select>
    <input type="text" class="ip-addr-input form-control" placeholder="IP 주소 (예: 192.168.1.10)" value="${ipAddress}" style="flex:1;">
    <button type="button" class="btn small danger btn-remove">X</button>`;
  row
    .querySelector(".btn-remove")
    .addEventListener("click", () => row.remove());
  ipListContainer.appendChild(row);
}

function createDiskRow(type = "SSD", capacity = 100, diskName = "") {
  const row = document.createElement("div");
  row.className = "disk-row form-row";
  row.style.cssText = "margin-bottom:0; display:flex; gap:10px;";
  row.innerHTML = `
    <select class="disk-type-sel form-control" style="flex:1;">
      <option value="HDD" ${type === "HDD" ? "selected" : ""}>HDD</option>
      <option value="SSD" ${type === "SSD" ? "selected" : ""}>SSD</option>
      <option value="NVME" ${type === "NVME" ? "selected" : ""}>NVMe</option>
      <option value="SAN" ${
        type === "SAN" ? "selected" : ""
      }>SAN 스토리지</option>
    </select>
    <input type="number" class="disk-cap-input form-control" placeholder="용량(GB)" value="${capacity}" min="1" style="flex:1;">
    <input type="text" class="disk-name-input form-control" placeholder="용도 (예: OS, Data)" value="${diskName}" style="flex:1.5;">
    <button type="button" class="btn small danger btn-remove">X</button>`;
  row
    .querySelector(".btn-remove")
    .addEventListener("click", () => row.remove());
  diskListContainer.appendChild(row);
}

function createSwRow(name = "", version = "") {
  const row = document.createElement("div");
  row.className = "sw-row form-row";
  row.style.cssText = "margin-bottom:0; display:flex; gap:10px;";
  row.innerHTML = `
    <input type="text" class="sw-name-input form-control" placeholder="S/W명 (예: Apache)" value="${name}" style="flex:1;">
    <input type="text" class="sw-ver-input form-control" placeholder="버전 (예: 2.4)" value="${version}" style="flex:1;">
    <button type="button" class="btn small danger btn-remove">X</button>`;
  row
    .querySelector(".btn-remove")
    .addEventListener("click", () => row.remove());
  swListContainer.appendChild(row);
}

// 검색 관련 모듈
// 🌟 2. 개별 필터 'X' 버튼 삭제 로직 (전역 함수로 등록하여 HTML 인라인에서 호출)
window.removeFilter = (type) => {
  activeFilters = activeFilters.filter((f) => f.type !== type);
  currentPage = 0;
  renderFilterChips();
  loadServers();
};

// 🌟 3. 필터 태그(Chips) 시각화 렌더링
function renderFilterChips() {
  if (activeFilters.length === 0) {
    filtersContainer.innerHTML =
      '<span style="color: #95a5a6; font-size: 0.9rem;">적용된 필터가 없습니다.</span>';
    return;
  }

  filtersContainer.innerHTML = activeFilters
    .map(
      (f) => `
        <span style="display: inline-flex; align-items: center; background: #eaf2f8; color: #2980b9; padding: 6px 14px; border-radius: 20px; font-size: 0.85rem; border: 1px solid #b3d4fc; box-shadow: 0 2px 4px rgba(0,0,0,0.05);">
            <strong style="margin-right: 6px; color:#2c3e50;">${f.label}:</strong> ${f.value}
            <span onclick="window.removeFilter('${f.type}')" style="margin-left: 10px; cursor: pointer; color: #e74c3c; font-weight: bold; font-size: 1rem;" title="이 필터 지우기">✖</span>
        </span>
    `
    )
    .join("");
}
