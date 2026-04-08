import { api } from "../common/api.js";
import { ui } from "../common/ui.js";

import { SearchFilter } from "../common/search-filter.js";
import { Pagination } from "../common/pagination.js";

// --- DOM Elements ---
const tableBody = document.getElementById("nd-table-body");

const inputId = document.getElementById("nd-id");
const selCategory = document.getElementById("nd-category");
const inputName = document.getElementById("nd-name");
const inputOS = document.getElementById("nd-os");
const selIpCidr = document.getElementById("nd-ip-cidr");
const inputIpAddress = document.getElementById("nd-ip-address");
const selHardware = document.getElementById("nd-hardware");
const inputDesc = document.getElementById("nd-desc");
const ipListContainer = document.getElementById("ip-list-container");

const btnOpenModal = document.getElementById("btn-open-modal");
const btnEdit = document.getElementById("btn-edit");
const btnDelete = document.getElementById("btn-delete");
const btnCancel = document.getElementById("btn-cancel");
const btnSave = document.getElementById("btn-save");
const btnAddIp = document.getElementById("btn-add-ip");

// --- State ---
let deviceList = [];
let hardwareList = [];
let cidrList = [];

let searchFilter, pagination;
let currentPage = 0;

document.addEventListener("DOMContentLoaded", async () => {
  const filterOptions = [
    { value: "name", label: "서버명 (Host)" },
    { value: "os", label: "운영체제 (OS)" },
    { value: "ipAddress", label: "관리 IP" },
  ];

  searchFilter = new SearchFilter(
    document.getElementById("network-search-filter"),
    filterOptions,
    () => {
      currentPage = 0;
      loadNetworkDevices();
    }
  );

  pagination = new Pagination(
    document.querySelector("#network-pagination .pagination-container"),
    (pageNo) => {
      currentPage = pageNo;
      loadNetworkDevices();
    }
  );
  await loadHardwares();
  await loadCidrs();
  await loadNetworkDevices();
  setupEventListeners();
});

// --- Event Listeners ---

function setupEventListeners() {
  btnOpenModal.addEventListener("click", openCreateModal);
  btnCancel.addEventListener("click", () => ui.closeModal("nd-modal"));
  btnSave.addEventListener("click", saveDevice);
  btnEdit.addEventListener("click", handleEditAction);
  btnDelete.addEventListener("click", handleDeleteAction);
  btnAddIp.addEventListener("click", () => createIpRow()); // 🌟 IP 추가 버튼

  ui.setupCheckAll("check-all", "nd-checkbox-item");
}

// --- API Loaders ---
async function loadHardwares() {
  try {
    const responseData = await api.get("/api/v1/hardwares?size=1000");
    hardwareList = responseData.content || []; // 🚨 핵심: .content 배열 추출
    // 네트워크 장비 계열만 필터링 (스위치, 라우터, 방화벽)
    const options = hardwareList
      .filter(
        (hw) =>
          hw.equipmentType === "SWITCH" ||
          hw.equipmentType === "ROUTER" ||
          hw.equipmentType === "FIREWALL"
      )
      .map(
        (hw) => `<option value="${hw.id}">[${hw.model}] ${hw.serialNo}</option>`
      )
      .join("");
    selHardware.innerHTML =
      `<option value="">-- 매핑 안 함 --</option>` + options;
  } catch (e) {
    console.error("하드웨어 로드 실패", e);
  }
}

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

async function loadNetworkDevices() {
  try {
    const filterParams = searchFilter.getQueryParams();
    const url = `/api/v1/network-devices?page=${currentPage}&size=20${filterParams}`;
    const responseData = await api.get(url);
    deviceList = responseData.content || [];

    renderTable();
    pagination.render(responseData.totalPages, responseData.number);
  } catch (e) {
    tableBody.innerHTML = `<tr><td colspan="6" style="text-align:center;color:red;">데이터 로드 실패</td></tr>`;
  }
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

// --- Render Logic ---
function getCategoryBadge(category) {
  const map = {
    ROUTER: "badge-router",
    SWITCH: "badge-switch",
    FIREWALL: "badge-firewall",
    L4_SWITCH: "badge-l4",
    L7_SWITCH: "badge-l7",
    VPN: "badge-vpn",
  };
  return map[category] || "badge-etc";
}

function renderTable() {
  if (deviceList.length === 0) {
    tableBody.innerHTML = `<tr><td colspan="6" style="text-align:center;">등록된 네트워크 장비가 없습니다.</td></tr>`;
    return;
  }

  tableBody.innerHTML = deviceList
    .map((dev) => {
      const hwInfo = dev.hardwareId
        ? (() => {
            const hw = hardwareList.find((h) => h.id === dev.hardwareId);
            return hw
              ? `<strong>${hw.model}</strong> <small>(${hw.serialNo})</small>`
              : "알 수 없음";
          })()
        : '<span style="color:#999;">미할당</span>';

      let ipDisplay = '<span style="color:#999;">미할당</span>';
      if (dev.ips && dev.ips.length > 0) {
        // N개의 IP를 줄바꿈(<br>)으로 표시
        ipDisplay = dev.ips
          .map((ipData) => `<strong>${ipData.ipAddress}</strong>`)
          .join("<br>");
      }

      const safeDesc = ui.escapeHtml(dev.description || "-");

      return `
            <tr>
                <td><input type="checkbox" class="data-checkbox nd-checkbox-item" data-id="${
                  dev.id
                }"></td>
                <td><strong>${dev.name}</strong></td>
                <td><span class="badge ${getCategoryBadge(dev.category)}">${
        dev.category
      }</span></td>
                <td>${dev.os}</td>
                <td>${ipDisplay}</td>
                <td>${hwInfo}</td>
                <td><div class="text-ellipsis" title="${safeDesc}">${safeDesc}</div></td>
            </tr>
        `;
    })
    .join("");
}

// --- Actions ---
function openCreateModal() {
  inputId.value = "";
  selCategory.value = "SWITCH";
  inputName.value = "";
  inputOS.value = "";
  selHardware.value = "";
  inputDesc.value = "";
  ipListContainer.innerHTML = ""; // 초기화
  createIpRow(); // 기본 1개 칸은 띄워줌

  ui.openModal("nd-modal", "modal-title", "새 네트워크 장비 등록");
}

function handleEditAction() {
  const ids = ui.getCheckedIds("nd-checkbox-item");
  if (ids.length !== 1) {
    alert("수정할 항목을 1개만 선택해주세요.");
    return;
  }
  openEditModal(parseInt(ids[0]));
}

function openEditModal(id) {
  const target = deviceList.find((d) => d.id === id);
  if (!target) return;

  inputId.value = target.id;
  selCategory.value = target.category;
  inputName.value = target.name;
  inputOS.value = target.os;
  selHardware.value = target.hardwareId || "";
  inputDesc.value = target.description || "";

  ipListContainer.innerHTML = "";
  if (target.ips && target.ips.length > 0) {
    target.ips.forEach((ipData) =>
      createIpRow(ipData.ipCidrId, ipData.ipAddress)
    );
  } else {
    createIpRow(); // 없으면 빈 칸 1개
  }

  ui.openModal("nd-modal", "modal-title", "네트워크 장비 수정");
}

async function saveDevice() {
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

  // ==============================================================
  // 🌟 2. 기존 정상 저장 로직
  // ==============================================================
  const hwIdVal = selHardware.value === "" ? null : parseInt(selHardware.value);

  const requestData = {
    category: selCategory.value,
    name: inputName.value.trim(),
    os: inputOS.value.trim(),
    ips: ips, // 🌟 List 전송
    hardwareId: hwIdVal,
    description: inputDesc.value.trim(),
  };

  try {
    if (inputId.value) {
      await api.put(`/api/v1/network-devices/${inputId.value}`, requestData);
      alert("수정되었습니다.");
    } else {
      await api.post("/api/v1/network-devices", requestData);
      alert("등록되었습니다.");
    }
    ui.closeModal("nd-modal");
    loadNetworkDevices();
    ui.clearCheckAll("check-all");
  } catch (error) {}
}

async function handleDeleteAction() {
  const ids = ui.getCheckedIds("nd-checkbox-item");
  if (ids.length === 0) {
    alert("삭제할 항목을 선택해주세요.");
    return;
  }
  if (!confirm(`선택한 ${ids.length}개의 장비를 삭제하시겠습니까?`)) return;

  try {
    for (const id of ids) await api.delete(`/api/v1/network-devices/${id}`);
    alert("삭제되었습니다.");
    loadNetworkDevices();
    ui.clearCheckAll("check-all");
  } catch (error) {}
}
