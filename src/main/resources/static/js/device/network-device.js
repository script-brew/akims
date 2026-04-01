import { api } from "../common/api.js";
import { ui } from "../common/ui.js";

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

const btnOpenModal = document.getElementById("btn-open-modal");
const btnEdit = document.getElementById("btn-edit");
const btnDelete = document.getElementById("btn-delete");
const btnCancel = document.getElementById("btn-cancel");
const btnSave = document.getElementById("btn-save");

// --- State ---
let deviceList = [];
let hardwareList = [];
let cidrList = [];

document.addEventListener("DOMContentLoaded", async () => {
  await loadHardwares();
  await loadCidrs();
  await loadNetworkDevices();
  setupEventListeners();
});

function setupEventListeners() {
  btnOpenModal.addEventListener("click", openCreateModal);
  btnCancel.addEventListener("click", () => ui.closeModal("nd-modal"));
  btnSave.addEventListener("click", saveDevice);
  btnEdit.addEventListener("click", handleEditAction);
  btnDelete.addEventListener("click", handleDeleteAction);

  ui.setupCheckAll("check-all", "nd-checkbox-item");
}

// --- API Loaders ---
async function loadHardwares() {
  try {
    hardwareList = await api.get("/api/v1/hardwares");
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

async function loadCidrs() {
  try {
    cidrList = await api.get("/api/v1/ip-cidrs");
    const options = cidrList
      .map(
        (cidr) =>
          `<option value="${cidr.id}">${cidr.cidrBlock} (${
            cidr.description || ""
          })</option>`
      )
      .join("");
    selIpCidr.innerHTML =
      `<option value="">-- 대역 선택 (연동 안함) --</option>` + options;
  } catch (e) {
    console.error("CIDR 로드 실패", e);
  }
}

async function loadNetworkDevices() {
  try {
    deviceList = await api.get("/api/v1/network-devices");
    renderTable();
  } catch (e) {
    tableBody.innerHTML = `<tr><td colspan="6" style="text-align:center;color:red;">데이터 로드 실패</td></tr>`;
  }
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
      if (dev.ipAddresses && dev.ipAddresses.length > 0) {
        ipDisplay = `<strong>${dev.ipAddresses[0]}</strong>`;
      } else if (dev.ipAddresses) {
        ipDisplay = `<strong>${dev.ipAddresses[0]}</strong>`;
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
  selIpCidr.value = "";
  inputIpAddress.value = "";
  selHardware.value = "";
  inputDesc.value = "";

  ui.openModal("nd-modal", "modal-title", "새 네트워크 장비 등록");
}

function handleEditAction() {
  const ids = ui.getCheckedIds("nd-checkbox-item");
  if (ids.length !== 1) {
    alert("수정할 항목을 1개만 선택해주세요.");
    return;
  }

  const target = deviceList.find((d) => d.id === parseInt(ids[0]));
  if (!target) return;

  inputId.value = target.id;
  selCategory.value = target.category;
  inputName.value = target.name;
  inputOS.value = target.os;
  selIpCidr.value = target.ipCidrId || "";
  inputIpAddress.value =
    target.assignedIps && target.assignedIps.length > 0
      ? target.assignedIps[0]
      : target.ipAddress || "";
  selHardware.value = target.hardwareId || "";
  inputDesc.value = target.description || "";

  ui.openModal("nd-modal", "modal-title", "네트워크 장비 수정");
}

async function saveDevice() {
  const hwIdVal = selHardware.value === "" ? null : parseInt(selHardware.value);
  const cidrIdVal = selIpCidr.value === "" ? null : parseInt(selIpCidr.value);

  const requestData = {
    category: selCategory.value,
    name: inputName.value.trim(),
    os: inputOS.value.trim(),
    ipCidrId: cidrIdVal,
    ipAddress: inputIpAddress.value.trim(),
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
