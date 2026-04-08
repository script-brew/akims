import { api } from "../common/api.js";
import { ui } from "../common/ui.js";

import { SearchFilter } from "../common/search-filter.js";
import { Pagination } from "../common/pagination.js";

// --- DOM Elements ---
const tableBody = document.getElementById("hardware-table-body");
const modalTitle = document.getElementById("modal-title");

// Form Inputs
const inputId = document.getElementById("hw-id");
const selType = document.getElementById("hw-equipment-type");
const inputSerial = document.getElementById("hw-serial");
const inputModel = document.getElementById("hw-model");
const inputYear = document.getElementById("hw-year");
const inputSize = document.getElementById("hw-size");
const chkSinglePower = document.getElementById("hw-single-power");
const groupPowerLine = document.getElementById("hw-power-line-group");
const selPowerLine = document.getElementById("hw-power-line");
const selRack = document.getElementById("hw-rack");
const inputPosition = document.getElementById("hw-position");
const inputDesc = document.getElementById("hw-desc");

const btnOpenModal = document.getElementById("btn-open-modal");
const btnCancel = document.getElementById("btn-cancel");
const btnSave = document.getElementById("btn-save");

const btnEdit = document.getElementById("btn-edit");
const btnDelete = document.getElementById("btn-delete");

const btnDownloadExcel = document.getElementById("btn-download-excel");
const btnUploadExcel = document.getElementById("btn-upload-excel");
const excelFileInput = document.getElementById("excel-file-input");

// --- State ---
let hardwareList = [];
let rackList = [];

let searchFilter, pagination;
let currentPage = 0;

// --- Init ---
document.addEventListener("DOMContentLoaded", async () => {
  const filterOptions = [
    { value: "model", label: "모델명" },
    { value: "serialNo", label: "시리얼 번호" },
  ];

  searchFilter = new SearchFilter(
    document.getElementById("hardware-search-filter"),
    filterOptions,
    () => {
      currentPage = 0;
      loadHardwares();
    }
  );

  pagination = new Pagination(
    document.querySelector("#hardware-pagination .pagination-container"),
    (pageNo) => {
      currentPage = pageNo;
      loadHardwares();
    }
  );
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
    const filterParams = searchFilter.getQueryParams();
    const url = `/api/v1/hardwares?page=${currentPage}&size=20${filterParams}`;

    const responseData = await api.get(url);
    hardwareList = responseData.content || []; // 🚨 핵심: .content 배열 추출

    hardwareList = hardwareList.filter((hw) => hw.model !== "shelf");
    renderTable();
    pagination.render(responseData.totalPages, responseData.number);
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
      console.log(targetRack);
      const locationName = targetRack
        ? targetRack.locationName
        : '<span style="color:#999;">미실장</span>';

      // 🌟 2. 랙 실장 위치 정보 조합
      const rackDisplayName = hw.rackNo
        ? `${hw.rackNo}`
        : targetRack
        ? `${targetRack.rackNo}`
        : "명칭미정";

      const rackInfo = hw.rackId
        ? `${rackDisplayName} / ${hw.rackPosition} Slot`
        : `<span style="color:#999;">창고 보관</span>`;

      // 🌟 3. 전원 상태 뱃지
      const powerBadge = hw.isSinglePower
        ? `<span class="badge badge-alert">단일전원</span>`
        : `<span class="badge" style="background:#2ecc71;">이중전원</span>`;

      // 🌟 4. 설명(이름) 정보
      const safeDesc = ui.escapeHtml(hw.description || "-");

      const introductionYear = hw.introductionYear;

      // 🌟 5. 요청하신 순서대로 HTML <tr> 태그 조립 (ID 컬럼 제거)
      return `
            <tr>
                <td><input type="checkbox" class="data-checkbox hw-checkbox-item" data-id="${
                  hw.id
                }"></td>
                <td><strong>${locationName}</strong></td>
                <td>${rackInfo}</td>
                <td><span class="badge ${getBadgeClass(hw.equipmentType)}">${
        hw.equipmentType
      }</span></td>
      <td>${introductionYear}</td>
      <td><strong>${hw.model}</strong></td>
      <td>${hw.serialNo}</td>
                <td><div class="text-ellipsis" title="${safeDesc}">${safeDesc}</div></td>
                <td>${powerBadge}</td>
            </tr>
        `;
    })
    .join("");
}

// --- Event Listeners ---
function setupEventListeners() {
  btnOpenModal.addEventListener("click", openCreateModal);
  btnCancel.addEventListener("click", () => ui.closeModal("hw-modal"));
  btnSave.addEventListener("click", saveHardware);

  // 🌟 상단 수정 버튼 이벤트
  btnEdit.addEventListener("click", handleEditAction);

  // 🌟 상단 삭제 버튼 이벤트
  btnDelete.addEventListener("click", handleDeleteAction);

  ui.setupCheckAll("check-all", "hw-checkbox-item");

  chkSinglePower.addEventListener("change", (e) => {
    groupPowerLine.style.display = e.target.checked ? "block" : "none";
    if (!e.target.checked) {
      selPowerLine.value = "";
    } else {
      selPowerLine.value = "A";
    }
  });

  btnDownloadExcel.addEventListener("click", () => {
    // 현재 검색된 필터 조건을 그대로 URL 파라미터로 가져옴
    const filterParams = searchFilter.getQueryParams();
    // 브라우저의 기본 다운로드 동작을 유도하기 위해 window.location.href 사용
    window.location.href = `/api/v1/hardwares/excel/download?${filterParams.replace(
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
      const response = await fetch("/api/v1/hardwares/excel/upload", {
        method: "POST",
        body: formData,
        // 주의: FormData 사용 시 Content-Type 헤더를 명시적으로 설정하지 않아야 브라우저가 boundary를 자동 생성합니다.
      });
      const result = await response.json();

      if (response.ok) {
        alert(response.message);
        loadHardwares(); // 업로드 성공 후 목록 새로고침
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
}

function openCreateModal() {
  inputId.value = "";
  selType.value = "SERVER";
  inputSerial.value = "";
  inputModel.value = "";
  inputYear.value = new Date().getFullYear();
  inputSize.value = 1;
  chkSinglePower.checked = false;
  groupPowerLine.style.display = "none";
  selPowerLine.value = "";
  selRack.value = "";
  inputPosition.value = "";
  inputDesc.value = "";

  ui.openModal("hw-modal", "modal-title", "하드웨어 장비 입고");
}

async function saveHardware() {
  // 빈 문자열인 경우 null 처리 (백엔드 Long 타입 매핑 에러 방지)
  const rackIdVal = selRack.value === "" ? null : parseInt(selRack.value);
  // 🚨 프론트엔드 방어 로직: 랙을 선택하지 않았다면 rackPosition은 무조건 null로 세팅
  let rackPosVal = null;
  if (rackIdVal !== null) {
    rackPosVal =
      inputPosition.value === "" ? null : parseInt(inputPosition.value);

    // 추가 검증: 랙을 골랐는데 위치를 안 적은 경우 알림
    if (rackPosVal === null || isNaN(rackPosVal)) {
      alert(
        "랙(Rack)을 선택하신 경우, 실장될 위치(U)를 반드시 입력해야 합니다."
      );
      return; // API 호출 중단
    }
  }

  const requestData = {
    equipmentType: selType.value,
    serialNo: inputSerial.value.trim(),
    model: inputModel.value.trim(),
    introductionYear: parseInt(inputYear.value),
    size: parseInt(inputSize.value),
    isSinglePower: chkSinglePower.checked,
    powerLine: chkSinglePower.checked ? selPowerLine.value : "DUAL",
    rackId: rackIdVal,
    rackPosition: rackPosVal,
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
    ui.closeModal("hw-modal");
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
  groupPowerLine.style.display = target.isSinglePower ? "block" : "none";
  selPowerLine.value = target.powerLine;
  selRack.value = target.rackId || "";
  inputPosition.value = target.rackPosition || "";
  inputDesc.value = target.description || "";

  ui.openModal("hw-modal", "modal-title", "하드웨어 정보 수정");
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
  const ids = ui.getCheckedIds("hw-checkbox-item"); // 🌟 ui 모듈로 선택된 ID 가져오기
  if (ids.length !== 1) {
    alert("수정할 항목을 1개만 선택해주세요.");
    return;
  }
  openEditModal(parseInt(ids[0]));
}

// --- 🌟 비즈니스 로직: 삭제 Action (Bulk Delete) ---
async function handleDeleteAction() {
  const ids = ui.getCheckedIds("hw-checkbox-item"); // 🌟 ui 모듈 사용
  if (ids.length === 0) {
    alert("삭제할 항목을 선택해주세요.");
    return;
  }

  if (!confirm(`선택한 ${ids.length}개의 장비를 정말 삭제하시겠습니까?`))
    return;

  try {
    for (const id of ids) await api.delete(`/api/v1/hardwares/${id}`);
    alert("삭제되었습니다.");
    loadHardwares();
    ui.clearCheckAll("check-all"); // 🌟 전체 선택 해제도 ui 모듈로
  } catch (error) {
    console.error("삭제 실패", error);
  }
}
