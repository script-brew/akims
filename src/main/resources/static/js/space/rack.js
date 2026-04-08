import { api } from "../common/api.js";
import { ui } from "../common/ui.js";

// --- DOM Elements ---
const gridContainer = document.getElementById("rack-grid-container");
const modal = document.getElementById("rack-modal");
const selLocation = document.getElementById("rack-location");
const inputRackNo = document.getElementById("rack-no");
const inputRackName = document.getElementById("rack-name");
const inputRackSize = document.getElementById("rack-size");

const tableBody = document.getElementById("rack-table-body");
const visualContainer = document.getElementById("visual-rack-container");
const visualTitle = document.getElementById("visual-rack-title");
const rackGridArea = document.getElementById("rack-grid-area");

const btnOpenModal = document.getElementById("btn-open-modal");
const btnCancel = document.getElementById("btn-cancel");
const btnSave = document.getElementById("btn-save");

// --- State ---
let rackList = [];
let locationList = [];
let hardwareList = []; // 🌟 하드웨어 목록 추가

// --- Init ---
document.addEventListener("DOMContentLoaded", async () => {
  await loadLocations();
  await loadHardwares(); // 하드웨어 데이터를 먼저 확보
  await loadRacks(); // 이후 랙을 렌더링하면서 하드웨어를 꽂음
  setupEventListeners();
});

// 시각화 닫기 버튼 이벤트
document.getElementById("btn-close-visual").addEventListener("click", () => {
  visualContainer.style.display = "none";
});

// --- API Calls & Render ---
async function loadLocations() {
  try {
    const responseData = await api.get("/api/v1/locations?size=1000");
    locationList = responseData.content || [];
    selLocation.innerHTML = locationList
      .map((loc) => `<option value="${loc.id}">${loc.name}</option>`)
      .join("");
  } catch (error) {
    console.error("장소 목록 로드 실패", error);
  }
}

// 🌟 추가: 하드웨어 전체 목록 조회
async function loadHardwares() {
  try {
    const responseData = await api.get("/api/v1/hardwares?size=1000");
    hardwareList = responseData.content || []; // 🚨 핵심: .content 배열 추출
  } catch (error) {
    console.error("하드웨어 데이터 로드 실패");
  }
}

async function loadRacks() {
  try {
    const responseData = await api.get("/api/v1/racks?size=1000");
    rackList = responseData.content || [];
    renderTable();
  } catch (error) {
    gridContainer.innerHTML = `<p style="color:red;">데이터 로드 실패</p>`;
  }
}

// 🌟 1. 테이블 렌더링 로직 (새로 추가)
function renderTable() {
  if (rackList.length === 0) {
    tableBody.innerHTML = `<tr><td colspan="7" style="text-align:center;">등록된 랙이 없습니다.</td></tr>`;
    return;
  }

  tableBody.innerHTML = rackList
    .map(
      (rack) => `
        <tr>
            <td><input type="checkbox" class="data-checkbox rack-checkbox-item" data-id="${
              rack.id
            }"></td>
            <td><strong>${ui.escapeHtml(rack.locationName || "-")}</strong></td>
            <td><strong>${ui.escapeHtml(rack.rackNo)}</strong></td>
            <td>${ui.escapeHtml(rack.name || "-")}</td>
            <td>${rack.size} U</td>
            <td><span class="badge" style="background:#3498db;">${
              rack.hardwareCount || 0
            } 대</span></td>
            <td>
                <button class="btn small" onclick="window.viewVisualRack(${
                  rack.id
                })">랙 보기</button>
            </td>
        </tr>
    `
    )
    .join("");
}

// 🌟 2. 단일 랙 시각화 렌더링 로직 (기존 렌더링을 버튼 클릭 이벤트로 변경)
window.viewVisualRack = (rackId) => {
  const targetRack = rackList.find((r) => r.id === rackId);
  if (!targetRack) return;

  // 제목 및 컨테이너 노출
  visualTitle.textContent = `랙 시각화 뷰 : ${targetRack.rackNo} (${
    targetRack.name || "-"
  }) / ${targetRack.size}U`;
  visualContainer.style.display = "block";

  const rackHwList = hardwareList.filter((hw) => hw.rackId === rackId);
  let slotsHTML = "";
  let skipUs = 0;

  // 우리가 예전에 완성했던 완벽한 3단 그리드 + 흰색 배경 로직 그대로 사용!
  for (let u = targetRack.size; u >= 1; u--) {
    if (skipUs > 0) {
      skipUs--;
      continue;
    }

    const hw = rackHwList.find((h) => h.rackPosition === u);

    if (hw) {
      let typeClass = "hw-default";
      if (hw.equipmentType === "SERVER") typeClass = "hw-server";
      if (hw.equipmentType === "SWITCH" || hw.equipmentType === "ROUTER")
        typeClass = "hw-switch";
      if (hw.equipmentType === "STORAGE" || hw.equipmentType === "NAS")
        typeClass = "hw-storage";

      const powerAlert = hw.isSinglePower
        ? `<span class="single-power-alert" title="단일 전원">◀</span>`
        : "";
      const hwName = ui.escapeHtml(hw.description || hw.equipmentType);
      const heightCss = `height: ${29 * hw.size}px;`;

      // 다중 U 나사선 레일 처리
      let uNumsHTML = `<div class="u-num-group">`;
      for (let i = 0; i < hw.size; i++) {
        uNumsHTML += `<div class="u-num">${u - i}U</div>`;
      }
      uNumsHTML += `</div>`;

      slotsHTML += `
                <div class="rack-slot" style="${heightCss}">
                    ${uNumsHTML}
                    <div class="hw-item-wrapper">
                        <div class="hw-item ${typeClass}">
                            <div class="hw-info hw-name" title="${hwName}">${hwName}</div>
                            <div class="hw-info hw-model" title="${ui.escapeHtml(
                              hw.model
                            )}">${ui.escapeHtml(hw.model)}</div>
                            <div class="hw-info hw-serial" title="${ui.escapeHtml(
                              hw.serialNo
                            )}">
                                ${powerAlert} ${ui.escapeHtml(hw.serialNo)}
                            </div>
                        </div>
                    </div>
                </div>
            `;
      skipUs = hw.size - 1;
    } else {
      slotsHTML += `
                <div class="rack-slot empty" style="height: 29px;">
                    <div class="u-num-group">
                        <div class="u-num">${u}U</div>
                    </div>
                    <div class="hw-item-wrapper empty-wrapper"></div>
                </div>
            `;
    }
  }

  // // 화면에 렌더링하고 부드럽게 스크롤
  // rackGridArea.innerHTML = `
  //       <div class="rack-cabinet" style="width: 450px;">
  //           ${slotsHTML}
  //       </div>
  //   `;
  // visualContainer.scrollIntoView({ behavior: "smooth", block: "start" });

  if (locRacks.length === 0) {
    // ... 데이터 없음 메시지 유지 ...
  } else {
    // 각 랙별로 캐비닛 HTML을 생성하여 가로로 이어붙임
    rackGridArea.innerHTML = locRacks
      .map((rack) => {
        const rackHws = hardwareList.filter((h) => h.rackId === rack.id);
        const slotsHtml = generateRackSlotsHTML(rack, rackHws);

        // 🌟 수정됨: 고정 너비(min-width)와 flex-shrink를 제거하고, 너비를 100%로 설정하여 그리드 칸에 맞춥니다.
        return `
                <div class="rack-cabinet-wrapper" style="width: 100%;">
                    <div style="text-align:center; margin-bottom: 12px; padding: 10px; background: #f1f2f6; border-radius: 6px; border: 1px solid #dcdde1;">
                        <div style="font-size: 1.2rem; font-weight: bold; color: #2c3e50;">${ui.escapeHtml(
                          rack.rackNo
                        )}</div>
                        <div style="color: #7f8c8d; font-size: 0.9rem; margin-bottom: 10px;">${ui.escapeHtml(
                          rack.name || "-"
                        )} (${rack.size}U)</div>
                        <div>
                            <button class="btn small" style="background-color:#f39c12;" onclick="window.editRack(${
                              rack.id
                            })">수정</button>
                            <button class="btn small danger" onclick="window.deleteRack(${
                              rack.id
                            })">삭제</button>
                        </div>
                    </div>
                    <div class="rack-cabinet" style="width: 100%;">
                        ${slotsHtml}
                    </div>
                </div>
            `;
      })
      .join("");
  }

  visualContainer.style.display = "block";
  visualContainer.scrollIntoView({ behavior: "smooth", block: "start" });
};

// --- Event Listeners ---
function setupEventListeners() {
  btnOpenModal.addEventListener("click", openCreateModal);
  btnCancel.addEventListener("click", () => ui.closeModal("rack-modal"));
  btnSave.addEventListener("click", saveRack);
}

function openCreateModal() {
  if (locationList.length === 0) {
    alert("등록된 장소(Location)가 없습니다. 장소를 먼저 등록해주세요.");
    return;
  }
  inputRackNo.value = "";
  inputRackName.value = "";
  inputRackSize.value = 42; // 기본 42U

  ui.openModal("rack-modal", "modal-title", "랙 등록");
}

function closeModal() {
  modal.style.display = "none";
}

async function saveRack() {
  const requestData = {
    locationId: parseInt(selLocation.value),
    rackNo: inputRackNo.value.trim(),
    name: inputRackName.value.trim(),
    size: parseInt(inputRackSize.value),
  };

  try {
    await api.post("/api/v1/racks", requestData);
    alert("등록되었습니다.");
    ui.closeModal("rack-modal");
    loadRacks();
  } catch (error) {
    // api.js에서 오류 처리 완료
  }
}

window.deleteRack = async (id) => {
  if (
    !confirm("정말 삭제하시겠습니까? (실장된 장비가 있으면 삭제할 수 없습니다)")
  )
    return;

  try {
    await api.delete(`/api/v1/racks/${id}`);
    alert("삭제되었습니다.");
    loadRacks();
  } catch (error) {
    // api.js에서 오류 처리 완료
  }
};
