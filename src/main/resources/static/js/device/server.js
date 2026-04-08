import { api } from "../common/api.js";
import { ui } from "../common/ui.js";
import { SearchFilter } from "../common/search-filter.js";
import { Pagination } from "../common/pagination.js";

// --- DOM Elements ---
const tableBody = document.getElementById("server-table-body");
const modalTitle = document.getElementById("modal-title");

// Form Inputs
const inputId = document.getElementById("srv-id");
const inputHostName = document.getElementById("srv-hostname");
const selEnvironment = document.getElementById("srv-environment");
const selCategory = document.getElementById("srv-category");
const selType = document.getElementById("srv-type");
const inputOs = document.getElementById("srv-os");
const inputCpu = document.getElementById("srv-cpu");
const inputRam = document.getElementById("srv-ram");
const selIpCidr = document.getElementById("srv-ip-cidr");
const inputIpAddress = document.getElementById("srv-ip-address");
const selHardware = document.getElementById("srv-hardware");
const inputDesc = document.getElementById("srv-desc");

const chkHa = document.getElementById("srv-ha");
const selBackup = document.getElementById("srv-backup");
const selMonitoring = document.getElementById("srv-monitoring");

const hardwareMappingArea = document.getElementById("hardware-mapping-area");
const ipListContainer = document.getElementById("ip-list-container");
const diskListContainer = document.getElementById("disk-list-container");
const swContainer = document.getElementById("sw-list-container");
const dbmsContainer = document.getElementById("dbms-list-container");

const detailContainer = document.getElementById("server-detail-container");
const detailTitle = document.getElementById("detail-title");
const detailContent = document.getElementById("server-detail-content");

// Buttons
const btnOpenModal = document.getElementById("btn-open-modal");
const btnEdit = document.getElementById("btn-edit");
const btnDelete = document.getElementById("btn-delete");
const btnCancel = document.getElementById("btn-cancel");
const btnSave = document.getElementById("btn-save");
const btnAddIp = document.getElementById("btn-add-ip");
const btnAddDisk = document.getElementById("btn-add-disk");
const btnAddSw = document.getElementById("btn-add-sw");
const btnCloseDetail = document.getElementById("btn-close-detail");

const paginationArea = document.getElementById("pagination-area");

const btnDownloadExcel = document.getElementById("btn-download-excel");
const btnUploadExcel = document.getElementById("btn-upload-excel");
const excelFileInput = document.getElementById("excel-file-input");

// --- State ---
let serverList = [];
let hardwareList = [];
let cidrList = [];

// 페이징 상태 관리
let currentPage = 0;
let activeFilters = [];
let searchFilter; // 🌟 필터 인스턴스 변수
let pagination; // 🌟 페이징 인스턴스 변수

// --- Init ---
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
      currentPage = 0; // 검색이 발생하면 1페이지로 리셋
      loadServers();
    }
  );

  // 🌟 페이징 인스턴스 초기화
  pagination = new Pagination(
    document.querySelector("#server-pagination .pagination-container"),
    (pageNo) => {
      // 버튼이 클릭되면 이 콜백이 실행됨!
      currentPage = pageNo;
      loadServers();
    }
  );

  await loadHardwares(); // 하드웨어 매핑용
  await loadCidrs(); // 🌟 IP 대역 로드
  await loadServers();
  setupEventListeners();
});

// 🌟 IP 대역 목록 로드 (Page 객체 대응)
async function loadCidrs() {
  try {
    const responseData = await api.get("/api/v1/ip-cidrs?size=1000");
    cidrList = responseData.content || []; // 🚨 핵심: .content 배열 추출

    const options = cidrList
      .map(
        (cidr) =>
          `<option value="${cidr.id}">${cidr.cidrBlock} (${
            cidr.description || "이름 없음"
          })</option>`
      )
      .join("");
    selIpCidr.innerHTML =
      `<option value="">-- 대역 선택 (연동 안함) --</option>` + options;
  } catch (error) {
    console.error("IP 대역 목록 로드 실패", error);
  }
}

// 🌟 하드웨어 매핑 목록 로드 (Page 객체 대응)
async function loadHardwares() {
  try {
    // 콤보박스에 전체를 띄우기 위해 size를 넉넉히 줍니다.
    const responseData = await api.get("/api/v1/hardwares?size=1000");
    hardwareList = responseData.content || []; // 🚨 핵심: .content 배열 추출

    const options = hardwareList
      .filter((hw) => hw.equipmentType === "SERVER")
      .map(
        (hw) => `<option value="${hw.id}">[${hw.model}] ${hw.serialNo}</option>`
      )
      .join("");
    selHardware.innerHTML =
      `<option value="">-- 매핑 안 함 --</option>` + options;
  } catch (error) {
    console.error("하드웨어 목록 로드 실패", error);
  }
}

async function loadServers() {
  try {
    // 🌟 searchFilter 인스턴스에게 현재 쌓인 파라미터 문자열을 달라고 요청!
    const filterParams = searchFilter.getQueryParams();
    const url = `/api/v1/servers?page=${currentPage}&size=20${filterParams}`;

    const responseData = await api.get(url);
    serverList = responseData.content || [];

    renderTable();
    pagination.render(responseData.totalPages, responseData.number);
  } catch (error) {
    tableBody.innerHTML = `<tr><td colspan="8" style="text-align:center;color:red;">데이터 로드 실패</td></tr>`;
  }
}

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

// --- Render Logic ---
function getOsClass(osName) {
  if (!osName) return "";
  const lower = osName.toLowerCase();
  if (lower.includes("win")) return "os-windows";
  if (
    lower.includes("linux") ||
    lower.includes("centos") ||
    lower.includes("ubuntu")
  )
    return "os-linux";
  return "";
}

function getTypeBadge(type) {
  if (type === "PHYSICAL") return "badge-physical";
  if (type === "VIRTUAL") return "badge-vm";
  if (type === "CLOUD") return "badge-cloud";
  return "";
}

// 🌟 동적으로 IP 입력 행(Row)을 생성하는 함수
function createIpRow(cidrId = "", ip = "") {
  const row = document.createElement("div");
  row.className = "ip-row form-row";
  row.style.marginBottom = "0";

  const options = cidrList
    .map(
      (cidr) =>
        `<option value="${cidr.id}" ${cidr.id == cidrId ? "selected" : ""}>${
          cidr.cidrBlock
        } (${cidr.description || "이름 없음"})</option>`
    )
    .join("");

  row.innerHTML = `
        <select class="ip-cidr-sel" style="flex:1;">
            <option value="">-- 대역 선택 --</option>
            ${options}
        </select>
        <input type="text" class="ip-address-input" placeholder="예: 10.10.10.15" value="${ip}" style="flex:1;">
        <button type="button" class="btn small danger btn-remove-ip">X</button>
    `;

  row
    .querySelector(".btn-remove-ip")
    .addEventListener("click", () => row.remove());
  ipListContainer.appendChild(row);
}

function createDiskRow(type = "SSD", capacity = 100, mount = "/") {
  const row = document.createElement("div");
  row.className = "disk-row form-row";
  row.style.marginBottom = "0";

  row.innerHTML = `
        <select class="disk-type-sel" style="flex:1;">
            <option value="HDD" ${type === "HDD" ? "selected" : ""}>HDD</option>
            <option value="SSD" ${type === "SSD" ? "selected" : ""}>SSD</option>
            <option value="NVME" ${
              type === "NVME" ? "selected" : ""
            }>NVMe</option>
            <option value="SAN" ${
              type === "SAN" ? "selected" : ""
            }>SAN 스토리지</option>
            <option value="NAS" ${
              type === "NAS" ? "selected" : ""
            }>NAS 마운트</option>
        </select>
        <input type="number" class="disk-cap-input" placeholder="용량(GB)" value="${capacity}" min="1" style="flex:1;">
        <input type="text" class="disk-mount-input" placeholder="마운트 (예: /data, C:)" value="${mount}" style="flex:1;">
        <button type="button" class="btn small danger btn-remove-disk">X</button>
    `;

  row
    .querySelector(".btn-remove-disk")
    .addEventListener("click", () => row.remove());
  diskListContainer.appendChild(row);
}

// 🌟 S/W 동적 행 생성
function createSwRow(name = "", version = "", purpose = "") {
  const row = document.createElement("div");
  row.className = "sw-row form-row";
  row.style.marginBottom = "0";
  row.innerHTML = `
        <input type="text" class="sw-name" placeholder="S/W명 (예: Tomcat)" value="${name}" style="flex:2;">
        <input type="text" class="sw-version" placeholder="버전" value="${version}" style="flex:1;">
        <input type="text" class="sw-purpose" placeholder="용도" value="${purpose}" style="flex:2;">
        <button type="button" class="btn small danger btn-remove">X</button>
    `;
  row.querySelector(".btn-remove").onclick = () => row.remove();
  swContainer.appendChild(row);
}

function renderTable() {
  if (serverList.length === 0) {
    tableBody.innerHTML = `<tr><td colspan="8" style="text-align:center;">등록된 서버가 없습니다.</td></tr>`;
    return;
  }

  tableBody.innerHTML = serverList
    .map((srv) => {
      // 하드웨어 호스트 정보 매핑
      const hwInfo = srv.hardwareId
        ? (() => {
            const hw = hardwareList.find((h) => h.id === srv.hardwareId);
            return hw
              ? `<strong>${hw.model}</strong><br><small style="color:#777;">(${hw.serialNo})</small>`
              : "알 수 없음";
          })()
        : '<span style="color:#999;">- (클라우드/미할당) -</span>';

      let ipDisplay = '<span style="color:#999;">미할당</span>';
      if (srv.ips && srv.ips.length > 0) {
        // N개의 IP를 줄바꿈(<br>)으로 표시
        ipDisplay = srv.ips
          .map((ipData) => `<strong>${ipData.ipAddress}</strong>`)
          .join("<br>");
      }
      const safeDesc = ui.escapeHtml(srv.description || "-");
      let diskDisplay =
        srv.disks && srv.disks.length > 0
          ? srv.disks
              .map((d) => `<strong>${d.diskType} / ${d.size}GB</strong>`)
              .join("<br>")
          : '<span style="color:#999;">-</span>';
      return `
            <tr>
                <td><input type="checkbox" class="data-checkbox srv-checkbox-item" data-id="${
                  srv.id
                }"></td>
                <td>${srv.environment}</td>
                
                <td><span class="badge ${getTypeBadge(srv.serverType)}">${
        srv.serverType
      }</span></td>
                <td>${srv.serverCategory}</td>
                <td>
                    <a href="#" onclick="window.viewServerDetail(${
                      srv.id
                    }); return false;" style="color: #2980b9; text-decoration: underline; font-weight: bold; font-size: 1.1rem;">
                        ${ui.escapeHtml(srv.hostName)}
                    </a>
                    <br><small style="color:#777;">${ui.escapeHtml(
                      srv.description || ""
                    )}</small>
                </td>
                <td><span class="os-badge ${getOsClass(srv.os)}">${
        srv.os
      }</span></td>
                
                <td>${srv.cpuCore} Core / ${srv.memoryGb} GB</td>
                <td>${ipDisplay}</td> <td>${hwInfo}</td>
            </tr>
        `;
    })
    .join("");
}

// --- Event Listeners ---
function setupEventListeners() {
  btnOpenModal.addEventListener("click", openCreateModal);
  btnCancel.addEventListener("click", () => ui.closeModal("srv-modal"));
  btnSave.addEventListener("click", saveServer);
  btnEdit.addEventListener("click", handleEditAction);
  btnDelete.addEventListener("click", handleDeleteAction);
  btnAddIp.addEventListener("click", () => createIpRow()); // 🌟 IP 추가 버튼
  btnAddDisk.addEventListener("click", () => createDiskRow()); // 🌟 디스크 추가 버튼
  btnAddSw.addEventListener("click", () => createSwRow());
  btnCloseDetail.addEventListener("click", () => {
    detailContainer.style.display = "none";
  });

  btnDownloadExcel.addEventListener("click", () => {
    // 현재 검색된 필터 조건을 그대로 URL 파라미터로 가져옴
    const filterParams = searchFilter.getQueryParams();
    // 브라우저의 기본 다운로드 동작을 유도하기 위해 window.location.href 사용
    window.location.href = `/api/v1/servers/excel/download?${filterParams.replace(
      /^&/,
      ""
    )}`;
  });

  // 엑셀 업로드 버튼 클릭 시 숨겨진 file input 클릭
  btnUploadExcel.addEventListener("click", () => {
    excelFileInput.click();
  });

  // 파일이 선택되면 즉시 서버로 업로드 (Multipart form-data)
  excelFileInput.addEventListener("change", async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    const formData = new FormData();
    formData.append("file", file);

    try {
      // api.js 모듈이 JSON 전송 전용일 경우, 아래와 같이 fetch를 직접 호출
      const response = await fetch("/api/v1/servers/excel/upload", {
        method: "POST",
        body: formData,
        // 주의: FormData 사용 시 Content-Type 헤더를 명시적으로 설정하지 않아야 브라우저가 boundary를 자동 생성합니다.
      });
      const result = await response.json();

      if (response.ok) {
        alert("엑셀 데이터가 성공적으로 반영되었습니다.\n" + response.message);
        loadServers(); // 업로드 성공 후 목록 새로고침
      } else {
        alert("엑셀 업로드 실패:\n" + response.message);
      }
    } catch (error) {
      console.error("엑셀 업로드 에러", error);
      alert("오류 발생: " + error.message);
    } finally {
      excelFileInput.value = ""; // 초기화하여 같은 파일 다시 선택 가능하게 함
    }
  });

  ui.setupCheckAll("check-all", "srv-checkbox-item");

  // 🌟 15년 차의 디테일: 서버 타입에 따른 동적 UI 변경
  selType.addEventListener("change", (e) => {
    if (e.target.value === "AWS" || e.target.value === "SCP") {
      hardwareMappingArea.style.display = "none"; // 클라우드는 물리 장비 선택 안 함
      selHardware.value = "";
    } else {
      hardwareMappingArea.style.display = "block"; // 물리/VM은 물리 장비 선택 창 노출
    }
  });
}

// --- Actions (Modal & API) ---
function openCreateModal() {
  inputId.value = "";
  selCategory.value = "WEB";
  inputHostName.value = "";
  selEnvironment.value = "PRD"; // 🌟 초기값
  selType.value = "VIRTUAL";
  inputOs.value = "";
  inputCpu.value = 1;
  inputRam.value = 0.5;
  selHardware.value = "";
  inputDesc.value = "";
  chkHa.checked = false;
  selBackup.value = "NO_BACKUP";
  selMonitoring.value = "NO_MONITORING";
  ipListContainer.innerHTML = ""; // 초기화
  createIpRow(); // 기본 1개 칸은 띄워줌
  diskListContainer.innerHTML = "";
  createDiskRow("SSD", 100, "/"); // 기본 OS 영역용으로 1개 띄워줌

  hardwareMappingArea.style.display = "block";
  ui.openModal("srv-modal", "modal-title", "새 서버 등록");
}

function openEditModal(id) {
  const target = serverList.find((s) => s.id === id);
  if (!target) return;

  inputId.value = target.id;
  selCategory.value = target.serverCategory;
  inputHostName.value = target.hostName;
  selEnvironment.value = target.environment || "PRD";
  selType.value = target.serverType;
  inputOs.value = target.os;
  inputCpu.value = target.cpuCore || target.spec?.cpuCore || 1;
  inputRam.value = target.memoryGb || target.spec?.memoryGb || 0.5;

  chkHa.checked = target.ha;
  selBackup.value = target.backupInfo || "NO_BACKUP";
  selMonitoring.value = target.monitoringInfo || "NO_MONITORING";

  ipListContainer.innerHTML = "";

  if (target.ips && target.ips.length > 0) {
    target.ips.forEach((ipData) =>
      createIpRow(ipData.ipCidrId, ipData.ipAddress)
    );
  } else {
    createIpRow(); // 없으면 빈 칸 1개
  }

  diskListContainer.innerHTML = "";
  if (target.disks && target.disks.length > 0) {
    target.disks.forEach((d) =>
      createDiskRow(d.diskType, d.size, d.mountPoint)
    );
  } else {
    createDiskRow();
  }
  swContainer.innerHTML = "";
  if (target.softwares && target.softwares.length > 0) {
    target.softwares.forEach((sw) =>
      createSwRow(sw.name, sw.version, sw.purpose)
    );
  }
  selHardware.value = target.hardwareId || "";
  inputDesc.value = target.description || "";

  hardwareMappingArea.style.display =
    target.serverType === "CLOUD" ? "none" : "block";
  ui.openModal("srv-modal", "modal-title", "서버 정보 수정");
}

function handleEditAction() {
  const ids = ui.getCheckedIds("srv-checkbox-item"); // 🌟 ui 모듈로 선택된 ID 가져오기
  if (ids.length !== 1) {
    alert("수정할 항목을 1개만 선택해주세요.");
    return;
  }
  openEditModal(parseInt(ids[0]));
}

function closeModal() {
  modal.style.display = "none";
}

async function saveServer() {
  // 🌟 1. 다중 IP 데이터 수집 및 방어 로직(Validation)
  const ips = [];
  let hasIpError = false;

  // 화면에 동적으로 생성된 모든 IP 입력 줄(.ip-row)을 검사합니다.
  const ipRows = document.querySelectorAll(".ip-row");

  for (const row of ipRows) {
    const cidrId = row.querySelector(".ip-cidr-sel").value;
    const ipAddr = row.querySelector(".ip-address-input").value.trim();

    // 케이스 A: 둘 다 비어있으면 (IP 할당 안 함) -> 통과 (무시)
    if (!cidrId && !ipAddr) continue;

    // 🚨 케이스 B (방어 로직): IP 주소는 썼는데 대역(CIDR)을 안 고른 경우
    if (!cidrId && ipAddr) {
      alert(`입력하신 IP(${ipAddr})의 대역(CIDR)을 선택해주세요.`);
      hasIpError = true;
      break; // 루프 중단
    }

    // 🚨 케이스 C (방어 로직): 대역은 골랐는데 IP 주소를 안 쓴 경우
    if (cidrId && !ipAddr) {
      alert("선택하신 대역에 할당할 IP 주소를 입력해주세요.");
      hasIpError = true;
      break; // 루프 중단
    }

    // 케이스 D: 둘 다 정상적으로 입력됨 -> 전송할 배열에 추가
    ips.push({ ipCidrId: parseInt(cidrId), ipAddress: ipAddr });
  }

  // 🌟 15년 차의 핵심: 유효성 검사 실패 시 백엔드 API를 호출하지 않고 함수 즉시 종료!
  if (hasIpError) return;

  const disks = [];
  document.querySelectorAll(".disk-row").forEach((row) => {
    const dType = row.querySelector(".disk-type-sel").value;
    const dCap = row.querySelector(".disk-cap-input").value;
    const dMount = row.querySelector(".disk-mount-input").value.trim();
    if (dType && dCap) {
      disks.push({
        diskType: dType,
        size: parseInt(dCap),
        mountPoint: dMount,
      });
    }
  });
  // S/W 데이터 수집
  const softwares = [];
  document.querySelectorAll(".sw-row").forEach((row) => {
    const n = row.querySelector(".sw-name").value.trim();
    const v = row.querySelector(".sw-version").value.trim();
    const p = row.querySelector(".sw-purpose").value.trim();
    if (n && v)
      softwares.push({
        name: n,
        version: v,
        purpose: p,
        maintenanceInfo: "",
      });
  });

  // ==============================================================
  // 🌟 2. 기존 정상 저장 로직
  // ==============================================================
  if (
    (selType.value === "PHYSICAL" || selType.value === "VIRTUAL") &&
    selHardware.value === ""
  ) {
    alert(`하드웨어 정보는 필수입니다!`);
    return; // 루프 중단
  }
  const hwIdVal = selHardware.value === "" ? null : parseInt(selHardware.value);

  const requestData = {
    hostName: inputHostName.value.trim(),
    environment: selEnvironment.value,
    serverCategory: selCategory.value,
    serverType: selType.value,
    os: inputOs.value.trim(),
    cpuCore: parseFloat(inputCpu.value),
    memoryGb: parseFloat(inputRam.value),
    hardwareId: hwIdVal,
    description: inputDesc.value.trim(),
    ha: chkHa.checked,
    backupInfo: selBackup.value.trim(),
    monitoringInfo: selMonitoring.value.trim(),
    ips: ips, // 🌟 검증을 무사히 통과한 N개의 IP 리스트 전송
    disks: disks,
    softwares: softwares,
  };

  try {
    if (inputId.value) {
      await api.put(`/api/v1/servers/${inputId.value}`, requestData);
      alert("수정되었습니다.");
    } else {
      await api.post("/api/v1/servers", requestData);
      alert("등록되었습니다.");
    }
    ui.closeModal("srv-modal");
    loadServers();
    ui.clearCheckAll("check-all");
  } catch (error) {
    // api.js가 백엔드 에러(예: 범위 불일치, 중복 등)를 처리하여 띄워줌
  }
}

async function handleDeleteAction() {
  const ids = ui.getCheckedIds("srv-checkbox-item"); // 🌟 ui 모듈 사용
  if (ids.length === 0) {
    alert("삭제할 항목을 선택해주세요.");
    return;
  }

  if (!confirm(`선택한 ${ids.length}개의 장비를 정말 삭제하시겠습니까?`))
    return;

  try {
    for (const id of ids) await api.delete(`/api/v1/servers/${id}`);
    alert("삭제되었습니다.");
    loadServers();
    ui.clearCheckAll("check-all"); // 🌟 전체 선택 해제도 ui 모듈로
  } catch (error) {
    console.error("삭제 실패", error);
  }
}

// 🌟 서버 상세 드릴다운 뷰 렌더링
window.viewServerDetail = (id) => {
  const srv = serverList.find((s) => s.id === id);
  if (!srv) return;

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
              `<div style="margin-bottom:4px;">[${d.diskType}] <strong>${d.size}GB</strong> <code style="font-size:0.8rem;">${d.mountPoint}</code></div>`
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
            <div style="margin-bottom: 8px;"><strong>OS:</strong> <span class="os-badge ${getOsClass(
              srv.os
            )}">${srv.os}</span></div>
            <div style="margin-bottom: 8px;"><strong>CPU/RAM:</strong> ${
              srv.cpuCore
            } Core / ${srv.memoryGb} GB</div>
            <div style="margin-bottom: 8px;"><strong>유형:</strong> <span class="badge ${getTypeBadge(
              srv.serverType
            )}">${srv.serverType}</span></div>
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
  detailContainer.style.display = "block";
  detailContainer.scrollIntoView({ behavior: "smooth", block: "start" });
};
