import { api } from "../common/api.js";

// --- DOM Elements ---
const tableBody = document.getElementById("hardware-table-body");
const modal = document.getElementById("hw-modal");
const modalTitle = document.getElementById("modal-title");

// Form Inputs
const inputId = document.getElementById("hw-id");
const selType = document.getElementById("hw-equipment-type");
const inputSerial = document.getElementById("hw-serial");
const inputModel = document.getElementById("hw-model");
const inputYear = document.getElementById("hw-year");
const inputSize = document.getElementById("hw-size");
const chkSinglePower = document.getElementById("hw-single-power");
const selRack = document.getElementById("hw-rack");
const inputPosition = document.getElementById("hw-position");
const inputDesc = document.getElementById("hw-desc");

const btnOpenModal = document.getElementById("btn-open-modal");
const btnCancel = document.getElementById("btn-cancel");
const btnSave = document.getElementById("btn-save");

const btnEdit = document.getElementById("btn-edit");
const btnDelete = document.getElementById("btn-delete");
const checkAll = document.getElementById("check-all");

// --- State ---
let hardwareList = [];
let rackList = [];

// --- Init ---
document.addEventListener("DOMContentLoaded", async () => {
  await loadRacks(); // 랙 목록을 먼저 불러와 실장 위치 콤보박스 세팅
  await loadHardwares(); // 하드웨어 목록 세팅
  setupEventListeners();
});

// --- API Calls & Render ---
async function loadRacks() {
  try {
    rackList = await api.get("/api/v1/racks");
    const options = rackList
      .map(
        (rack) =>
          `<option value="${rack.id}">[${rack.locationName}] ${rack.name} (${rack.rackNo})</option>`
      )
      .join("");
    selRack.innerHTML =
      `<option value="">-- 미실장 (창고 보관) --</option>` + options;
  } catch (error) {
    console.error("랙 목록 로드 실패", error);
  }
}

async function loadHardwares() {
  try {
    hardwareList = await api.get("/api/v1/hardwares");
    renderTable();
  } catch (error) {
    tableBody.innerHTML = `<tr><td colspan="7" style="text-align:center;color:red;">데이터 로드 실패</td></tr>`;
  }
}

function getBadgeClass(type) {
  if (type === "SERVER") return "badge-server";
  if (type === "SWITCH" || type === "ROUTER") return "badge-switch";
  if (type === "STORAGE") return "badge-storage";
  return "badge-default";
}

function renderTable() {
  if (hardwareList.length === 0) {
    tableBody.innerHTML = `<tr><td colspan="8" style="text-align:center;">등록된 하드웨어가 없습니다.</td></tr>`;
    return;
  }

  tableBody.innerHTML = hardwareList
    .map((hw) => {
      // 🌟 1. Location(장소) 정보 추출: 이미 로드된 rackList에서 해당 하드웨어가 꽂힌 랙을 찾아 장소명을 가져옵니다.
      const targetRack = rackList.find((r) => r.id === hw.rackId);
      const locationName = targetRack
        ? targetRack.locationName
        : '<span style="color:#999;">미실장</span>';

      // 🌟 2. 랙 실장 위치 정보 조합
      const rackDisplayName = hw.rackNo
        ? `${hw.rackNo}(${hw.rackName || ""})`
        : targetRack
        ? `${targetRack.rackNo}`
        : "명칭미정";

      const rackInfo = hw.rackId
        ? `${rackDisplayName}-${hw.rackPosition}U`
        : `<span style="color:#999;">창고 보관</span>`;

      // 🌟 3. 전원 상태 뱃지
      const powerBadge = hw.isSinglePower
        ? `<span class="badge badge-alert">단일전원</span>`
        : `<span class="badge" style="background:#2ecc71;">이중전원</span>`;

      // 🌟 4. 설명(이름) 정보
      const description = hw.description || "-";

      const introductionYear = hw.introductionYear;

      // 🌟 5. 요청하신 순서대로 HTML <tr> 태그 조립 (ID 컬럼 제거)
      return `
            <tr>
                <td><input type="checkbox" class="hw-checkbox hw-checkbox-item" data-id="${
                  hw.id
                }"></td> <td><strong>${locationName}</strong></td>
                <td><span class="badge ${getBadgeClass(hw.equipmentType)}">${
        hw.equipmentType
      }</span></td>
                <td><div class="text-ellipsis" title="${description}">${description}</div></td>
                <td><strong>${hw.model}</strong></td>
                <td>${hw.serialNo}</td>
                <td>${introductionYear}</td>}
                <td>${rackInfo}</td>
                <td>${powerBadge}</td>
            </tr>
        `;
    })
    .join("");
}

// --- Event Listeners ---
function setupEventListeners() {
  btnOpenModal.addEventListener("click", openCreateModal);
  btnCancel.addEventListener("click", closeModal);
  btnSave.addEventListener("click", saveHardware);

  // 🌟 상단 수정 버튼 이벤트
  btnEdit.addEventListener("click", handleEditAction);

  // 🌟 상단 삭제 버튼 이벤트
  btnDelete.addEventListener("click", handleDeleteAction);

  // 🌟 전체 선택/해제 이벤트
  checkAll.addEventListener("change", (e) => {
    const checkboxes = document.querySelectorAll(".hw-checkbox-item");
    checkboxes.forEach((cb) => (cb.checked = e.target.checked));
  });
}

function openCreateModal() {
  inputId.value = "";
  selType.value = "SERVER";
  inputSerial.value = "";
  inputModel.value = "";
  inputYear.value = new Date().getFullYear();
  inputSize.value = 1;
  chkSinglePower.checked = false;
  selRack.value = "";
  inputPosition.value = "";
  inputDesc.value = "";

  modalTitle.textContent = "하드웨어 장비 입고";
  modal.style.display = "flex";
}

function closeModal() {
  modal.style.display = "none";
}

async function saveHardware() {
  // 빈 문자열인 경우 null 처리 (백엔드 Long 타입 매핑 에러 방지)
  const rackIdVal = selRack.value === "" ? null : parseInt(selRack.value);
  const positionVal =
    inputPosition.value === "" ? null : parseInt(inputPosition.value);

  const requestData = {
    equipmentType: selType.value,
    serialNo: inputSerial.value.trim(),
    model: inputModel.value.trim(),
    introductionYear: parseInt(inputYear.value),
    size: parseInt(inputSize.value),
    isSinglePower: chkSinglePower.checked,
    rackId: rackIdVal,
    rackPosition: positionVal,
    description: inputDesc.value.trim(),
  };

  const id = inputId.value;

  try {
    if (id) {
      await api.put(`/api/v1/hardwares/${id}`, requestData);
      alert("수정되었습니다.");
    } else {
      await api.post("/api/v1/hardwares", requestData);
      alert("등록되었습니다.");
    }
    closeModal();
    loadHardwares();
  } catch (error) {
    // api.js에서 오류 처리
  }
}

function openEditModal(id) {
  const target = hardwareList.find((hw) => hw.id === id);
  if (!target) return;

  inputId.value = target.id;
  selType.value = target.equipmentType;
  inputSerial.value = target.serialNo;
  inputModel.value = target.model;
  inputYear.value = target.introductionYear;
  inputSize.value = target.size;
  chkSinglePower.checked = target.isSinglePower;
  selRack.value = target.rackId || "";
  inputPosition.value = target.rackPosition || "";
  inputDesc.value = target.description || "";

  modalTitle.textContent = "하드웨어 정보 수정";
  modal.style.display = "flex";
}

window.deleteHardware = async (id) => {
  if (
    !confirm(
      "정말 삭제하시겠습니까? (이 하드웨어에 맵핑된 서버/네트워크 장비가 있으면 삭제되지 않습니다)"
    )
  )
    return;

  try {
    await api.delete(`/api/v1/hardwares/${id}`);
    alert("삭제되었습니다.");
    loadHardwares();
  } catch (error) {
    // api.js에서 오류 처리
  }
};

// --- 🌟 비즈니스 로직: 수정 Action ---
function handleEditAction() {
  const checkedBoxes = document.querySelectorAll(".hw-checkbox-item:checked");

  if (checkedBoxes.length === 0) {
    alert("수정할 항목을 선택해주세요.");
    return;
  }

  if (checkedBoxes.length > 1) {
    alert("수정은 한 번에 하나의 항목만 가능합니다.");
    return;
  }

  const id = checkedBoxes[0].getAttribute("data-id");
  openEditModal(parseInt(id));
}

// --- 🌟 비즈니스 로직: 삭제 Action (Bulk Delete) ---
async function handleDeleteAction() {
  const checkedBoxes = document.querySelectorAll(".hw-checkbox-item:checked");

  if (checkedBoxes.length === 0) {
    alert("삭제할 항목을 선택해주세요.");
    return;
  }

  const ids = Array.from(checkedBoxes).map((cb) => cb.getAttribute("data-id"));

  // 팝업창 재차 확인
  if (
    !confirm(
      `선택한 ${ids.length}개의 장비를 정말 삭제하시겠습니까?\n이 작업은 되돌릴 수 없습니다.`
    )
  ) {
    return;
  }

  try {
    // 루프를 돌며 삭제 처리 (백엔드에 벌크 삭제 API가 있다면 하나로 합칠 수 있음)
    for (const id of ids) {
      await api.delete(`/api/v1/hardwares/${id}`);
    }
    alert("성공적으로 삭제되었습니다.");
    loadHardwares();
    checkAll.checked = false; // 전체 선택 해제
  } catch (error) {
    console.error("삭제 중 오류 발생", error);
  }
}
