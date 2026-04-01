import { api } from "../common/api.js";
import { ui } from "../common/ui.js";

// --- DOM Elements ---
const tableBody = document.getElementById("location-table-body");
const modal = document.getElementById("location-modal");
const modalTitle = document.getElementById("modal-title");
const inputId = document.getElementById("location-id");
const inputName = document.getElementById("location-name");

const btnOpenModal = document.getElementById("btn-open-modal");
const btnCancel = document.getElementById("btn-cancel");
const btnSave = document.getElementById("btn-save");

// --- State (상태 관리) ---
let locationList = [];

// --- Init ---
document.addEventListener("DOMContentLoaded", () => {
  loadLocations();
  setupEventListeners();
});

// --- API Calls & Render ---
async function loadLocations() {
  try {
    locationList = await api.get("/api/v1/locations");
    renderTable();
  } catch (error) {
    tableBody.innerHTML = `<tr><td colspan="3" style="text-align:center;color:red;">데이터 로드 실패</td></tr>`;
  }
}

function renderTable() {
  if (locationList.length === 0) {
    tableBody.innerHTML = `<tr><td colspan="3" style="text-align:center;">등록된 장소가 없습니다.</td></tr>`;
    return;
  }

  tableBody.innerHTML = locationList
    .map(
      (loc) => `
        <tr>
            <td>${loc.id}</td>
            <td>${loc.name}</td>
            <td>
                <button class="btn small" onclick="window.editLocation(${loc.id})">수정</button>
                <button class="btn small danger" onclick="window.deleteLocation(${loc.id})">삭제</button>
            </td>
        </tr>
    `
    )
    .join("");
}

// --- Event Listeners ---
function setupEventListeners() {
  btnOpenModal.addEventListener("click", openCreateModal);
  btnCancel.addEventListener("click", () => ui.closeModal("location-modal"));
  btnSave.addEventListener("click", saveLocation);
}

// 모달 제어
function openCreateModal() {
  inputId.value = "";
  inputName.value = "";

  ui.openModal("location-modal", "modal-title", "장소 등록");
}

function closeModal() {
  modal.style.display = "none";
}

// 🌟 등록 및 수정 로직 (Save)
async function saveLocation() {
  const requestData = { name: inputName.value.trim() };
  const id = inputId.value;

  try {
    if (id) {
      // 수정 (PUT)
      await api.put(`/api/v1/locations/${id}`, requestData);
      alert("수정되었습니다.");
    } else {
      // 등록 (POST)
      await api.post("/api/v1/locations", requestData);
      alert("등록되었습니다.");
    }
    ui.closeModal("location-modal");
    loadLocations(); // 저장 후 목록 새로고침
  } catch (error) {
    // api.js에서 이미 에러 alert을 띄우므로 여기선 팝업을 닫지 않고 유지
    console.error("저장 실패", error);
  }
}

// 🌟 삭제 로직 (Delete) - 모듈 내부 함수를 전역에서 호출할 수 있도록 window 객체에 바인딩
window.deleteLocation = async (id) => {
  if (!confirm("정말 삭제하시겠습니까? (하위 랙이 존재하면 삭제되지 않습니다)"))
    return;

  try {
    await api.delete(`/api/v1/locations/${id}`);
    alert("삭제되었습니다.");
    loadLocations(); // 삭제 후 목록 새로고침
  } catch (error) {
    console.error("삭제 실패", error);
  }
};

window.editLocation = (id) => {
  const target = locationList.find((loc) => loc.id === id);
  if (!target) return;

  inputId.value = target.id;
  inputName.value = target.name;

  ui.openModal("location-modal", "modal-title", "장소 수정");
};
