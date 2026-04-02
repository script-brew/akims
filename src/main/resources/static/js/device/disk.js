import { api } from "../common/api.js";
import { ui } from "../common/ui.js";

// --- DOM Elements ---
const tableBody = document.getElementById("disk-table-body");
const inputId = document.getElementById("disk-id");
const selServer = document.getElementById("disk-server-id");
const selType = document.getElementById("disk-type");
const inputCapacity = document.getElementById("disk-capacity");
const inputMount = document.getElementById("disk-mount");
const serverSelectGroup = document.getElementById("server-select-group");

const btnOpenModal = document.getElementById("btn-open-modal");
const btnEdit = document.getElementById("btn-edit");
const btnDelete = document.getElementById("btn-delete");
const btnCancel = document.getElementById("btn-cancel");
const btnSave = document.getElementById("btn-save");

// --- State ---
let diskList = [];
let serverList = [];

document.addEventListener("DOMContentLoaded", async () => {
  await loadServers(); // 서버 목록 먼저 로드
  await loadDisks();
  setupEventListeners();
});

function setupEventListeners() {
  btnOpenModal.addEventListener("click", openCreateModal);
  btnCancel.addEventListener("click", () => ui.closeModal("disk-modal"));
  btnSave.addEventListener("click", saveDisk);
  btnEdit.addEventListener("click", handleEditAction);
  btnDelete.addEventListener("click", handleDeleteAction);

  ui.setupCheckAll("check-all", "disk-checkbox-item");
}

// --- API Loaders ---
async function loadServers() {
  try {
    serverList = await api.get("/api/v1/servers");
    const options = serverList
      .map((s) => `<option value="${s.id}">${s.hostName} (${s.os})</option>`)
      .join("");
    selServer.innerHTML = `<option value="">-- 서버 선택 --</option>` + options;
  } catch (e) {
    console.error("서버 목록 로드 실패", e);
  }
}

async function loadDisks() {
  try {
    diskList = await api.get("/api/v1/disks");
    renderTable();
  } catch (e) {
    tableBody.innerHTML = `<tr><td colspan="6" style="text-align:center;color:red;">데이터 로드 실패</td></tr>`;
  }
}

// --- Render ---
function renderTable() {
  if (diskList.length === 0) {
    tableBody.innerHTML = `<tr><td colspan="6" style="text-align:center;">등록된 디스크가 없습니다.</td></tr>`;
    return;
  }

  tableBody.innerHTML = diskList
    .map(
      (disk) => `
        <tr>
            <td><input type="checkbox" class="data-checkbox disk-checkbox-item" data-id="${
              disk.id
            }"></td>
            <td><strong>${ui.escapeHtml(
              disk.serverName || "알수없음"
            )}</strong></td>
            <td><span class="badge" style="background:#8e44ad;">${
              disk.diskType
            }</span></td>
            <td><strong>${disk.size} GB</strong></td>
            <td><code>${ui.escapeHtml(disk.mountPoint)}</code></td>
        </tr>
    `
    )
    .join("");
}

// --- Actions ---
function openCreateModal() {
  inputId.value = "";
  selServer.value = "";
  selType.value = "SSD";
  inputCapacity.value = 100;
  inputMount.value = "";

  serverSelectGroup.style.display = "block"; // 신규 등록시는 서버 선택 가능
  ui.openModal("disk-modal", "modal-title", "새 디스크 등록");
}

function handleEditAction() {
  const ids = ui.getCheckedIds("disk-checkbox-item");
  if (ids.length !== 1) {
    alert("수정할 항목을 1개만 선택해주세요.");
    return;
  }

  const target = diskList.find((d) => d.id === parseInt(ids[0]));
  if (!target) return;

  inputId.value = target.id;
  selServer.value = target.serverId;
  selType.value = target.diskType;
  inputCapacity.value = target.size;
  inputMount.value = target.mountPoint;

  serverSelectGroup.style.display = "none"; // 수정시는 서버 변경 불가 (정합성 유지)
  ui.openModal("disk-modal", "modal-title", "디스크 정보 수정");
}

async function saveDisk() {
  // Validation
  if (!inputId.value && !selServer.value) {
    alert("대상 서버를 선택해주세요.");
    return;
  }
  if (!inputMount.value.trim()) {
    alert("마운트 경로를 입력해주세요.");
    return;
  }

  const requestData = {
    serverId: parseInt(selServer.value),
    diskType: selType.value,
    size: parseInt(inputCapacity.value),
    mountPoint: inputMount.value.trim(),
  };

  try {
    if (inputId.value) {
      await api.put(`/api/v1/disks/${inputId.value}`, requestData);
      alert("수정되었습니다.");
    } else {
      await api.post("/api/v1/disks", requestData);
      alert("등록되었습니다.");
    }
    ui.closeModal("disk-modal");
    loadDisks();
    ui.clearCheckAll("check-all");
  } catch (error) {}
}

async function handleDeleteAction() {
  const ids = ui.getCheckedIds("disk-checkbox-item");
  if (ids.length === 0) {
    alert("삭제할 항목을 선택해주세요.");
    return;
  }
  if (!confirm(`선택한 ${ids.length}개의 디스크를 삭제하시겠습니까?`)) return;

  try {
    for (const id of ids) await api.delete(`/api/v1/disks/${id}`);
    alert("삭제되었습니다.");
    loadDisks();
    ui.clearCheckAll("check-all");
  } catch (error) {}
}
