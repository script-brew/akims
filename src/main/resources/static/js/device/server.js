import { api } from "../common/api.js";

// --- DOM Elements ---
const tableBody = document.getElementById("server-table-body");
const modal = document.getElementById("srv-modal");
const modalTitle = document.getElementById("modal-title");

// Form Inputs
const inputId = document.getElementById("srv-id");
const inputHostName = document.getElementById("srv-hostname");
const selCategory = document.getElementById("srv-category");
const selType = document.getElementById("srv-type");
const inputOs = document.getElementById("srv-os");
const inputCpu = document.getElementById("srv-cpu");
const inputRam = document.getElementById("srv-ram");
const selHardware = document.getElementById("srv-hardware");
const inputDesc = document.getElementById("srv-desc");
const hardwareMappingArea = document.getElementById("hardware-mapping-area");

// Buttons
const btnOpenModal = document.getElementById("btn-open-modal");
const btnEdit = document.getElementById("btn-edit");
const btnDelete = document.getElementById("btn-delete");
const btnCancel = document.getElementById("btn-cancel");
const btnSave = document.getElementById("btn-save");
const checkAll = document.getElementById("check-all");

// --- State ---
let serverList = [];
let hardwareList = [];

// --- Init ---
document.addEventListener("DOMContentLoaded", async () => {
  await loadHardwares(); // 하드웨어 매핑용
  await loadServers();
  setupEventListeners();
});

// --- API Calls ---
async function loadHardwares() {
  try {
    hardwareList = await api.get("/api/v1/hardwares");
    // 서버가 탑재될 수 있는 장비(주로 서버 타입 장비)만 필터링해서 보여주면 더 좋습니다.
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
    serverList = await api.get("/api/v1/servers");
    renderTable();
  } catch (error) {
    tableBody.innerHTML = `<tr><td colspan="8" style="text-align:center;color:red;">데이터 로드 실패</td></tr>`;
  }
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

      return `
            <tr>
                <td><input type="checkbox" class="srv-checkbox srv-checkbox-item" data-id="${
                  srv.id
                }"></td>
                <td><strong>${
                  srv.hostName
                }</strong><br><small style="color:#777;">${
        srv.description || ""
      }</small></td>
                <td><span class="os-badge ${getOsClass(srv.os)}">${
        srv.os
      }</span></td>
                <td><span class="badge ${getTypeBadge(srv.serverType)}">${
        srv.serverType
      }</span></td>
                <td>${srv.cpuCore} Core / ${srv.memoryGb} GB</td>
                <td><span style="color:#999;">IP 연동 예정</span></td>
                <td>${hwInfo}</td>
                <td>${srv.serverCategory}</td>
            </tr>
        `;
    })
    .join("");
}

// --- Event Listeners ---
function setupEventListeners() {
  btnOpenModal.addEventListener("click", openCreateModal);
  btnCancel.addEventListener("click", closeModal);
  btnSave.addEventListener("click", saveServer);
  btnEdit.addEventListener("click", handleEditAction);
  btnDelete.addEventListener("click", handleDeleteAction);

  checkAll.addEventListener("change", (e) => {
    document
      .querySelectorAll(".srv-checkbox-item")
      .forEach((cb) => (cb.checked = e.target.checked));
  });

  // 🌟 15년 차의 디테일: 서버 타입에 따른 동적 UI 변경
  selType.addEventListener("change", (e) => {
    if (e.target.value === "CLOUD") {
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
  inputHostName.value = "";
  selCategory.value = "WEB";
  selType.value = "VIRTUAL";
  inputOs.value = "";
  inputCpu.value = 4;
  inputRam.value = 8;
  selHardware.value = "";
  inputDesc.value = "";

  // 초기 동적 UI 세팅
  hardwareMappingArea.style.display = "block";

  modalTitle.textContent = "새 서버 등록";
  modal.style.display = "flex";
}

function handleEditAction() {
  const checkedBoxes = document.querySelectorAll(".srv-checkbox-item:checked");
  if (checkedBoxes.length !== 1) {
    alert("수정할 서버를 1개만 선택해주세요.");
    return;
  }

  const id = parseInt(checkedBoxes[0].getAttribute("data-id"));
  const target = serverList.find((s) => s.id === id);
  if (!target) return;

  inputId.value = target.id;
  inputHostName.value = target.hostName;
  selCategory.value = target.serverCategory;
  selType.value = target.serverType;
  inputOs.value = target.os;
  inputCpu.value = target.cpuCore;
  inputRam.value = target.memoryGb;
  selHardware.value = target.hardwareId || "";
  inputDesc.value = target.description || "";

  // 동적 UI 세팅
  hardwareMappingArea.style.display =
    target.serverType === "CLOUD" ? "none" : "block";

  modalTitle.textContent = "서버 정보 수정";
  modal.style.display = "flex";
}

function closeModal() {
  modal.style.display = "none";
}

async function saveServer() {
  const hwIdVal = selHardware.value === "" ? null : parseInt(selHardware.value);

  const requestData = {
    hostName: inputHostName.value.trim(),
    serverCategory: selCategory.value,
    serverType: selType.value,
    os: inputOs.value.trim(),
    cpuCore: parseInt(inputCpu.value),
    memoryGb: parseInt(inputRam.value),
    hardwareId: hwIdVal,
    description: inputDesc.value.trim(),
  };

  try {
    if (inputId.value) {
      await api.put(`/api/v1/servers/${inputId.value}`, requestData);
      alert("수정되었습니다.");
    } else {
      await api.post("/api/v1/servers", requestData);
      alert("등록되었습니다.");
    }
    closeModal();
    loadServers();
    checkAll.checked = false;
  } catch (error) {
    // api.js가 에러 처리
  }
}

async function handleDeleteAction() {
  const checkedBoxes = document.querySelectorAll(".srv-checkbox-item:checked");
  if (checkedBoxes.length === 0) {
    alert("삭제할 서버를 선택해주세요.");
    return;
  }

  const ids = Array.from(checkedBoxes).map((cb) => cb.getAttribute("data-id"));
  if (!confirm(`선택한 ${ids.length}대의 서버를 삭제하시겠습니까?`)) return;

  try {
    for (const id of ids) {
      await api.delete(`/api/v1/servers/${id}`);
    }
    alert("삭제되었습니다.");
    loadServers();
    checkAll.checked = false;
  } catch (error) {
    console.error("서버 삭제 실패", error);
  }
}
