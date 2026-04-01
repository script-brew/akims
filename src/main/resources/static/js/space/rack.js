import { api } from "../common/api.js";
import { ui } from "../common/ui.js";

// --- DOM Elements ---
const gridContainer = document.getElementById("rack-grid-container");
const modal = document.getElementById("rack-modal");
const selLocation = document.getElementById("rack-location");
const inputRackNo = document.getElementById("rack-no");
const inputRackName = document.getElementById("rack-name");
const inputRackSize = document.getElementById("rack-size");

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

// --- API Calls & Render ---
async function loadLocations() {
  try {
    locationList = await api.get("/api/v1/locations");
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
    hardwareList = await api.get("/api/v1/hardwares");
  } catch (error) {
    console.error("하드웨어 데이터 로드 실패");
  }
}

async function loadRacks() {
  try {
    rackList = await api.get("/api/v1/racks");
    renderVisualRacks();
  } catch (error) {
    gridContainer.innerHTML = `<p style="color:red;">데이터 로드 실패</p>`;
  }
}

// 🌟 15년 차의 핵심: 여러 U를 차지하는 하드웨어 렌더링 알고리즘
function renderVisualRacks() {
  if (rackList.length === 0) {
    gridContainer.innerHTML = `<p>등록된 랙(Rack)이 없습니다.</p>`;
    return;
  }

  gridContainer.innerHTML = rackList
    .map((rack) => {
      // 현재 랙에 실장된 하드웨어 필터링
      const hwInThisRack = hardwareList.filter((hw) => hw.rackId === rack.id);

      let slotsHTML = "";
      let skipUs = 0; // 하드웨어 크기(Size)에 따라 스킵할 U 갯수

      // 랙의 최상단(예: 42U)부터 바닥(1U)으로 내려오면서 그립니다.
      for (let u = rack.size; u >= 1; u--) {
        // 이미 위쪽 하드웨어가 차지한 영역이면 HTML을 그리지 않고 건너뜁니다.
        if (skipUs > 0) {
          skipUs--;
          continue;
        }

        // 하드웨어 위치 매핑: 보통 인프라 자산 관리는 하단 위치(rackPosition)를 기준으로 합니다.
        // 최상단 U를 찾기 위해 (rackPosition + size - 1) 을 계산합니다.
        const hw = hwInThisRack.find((h) => h.rackPosition + h.size - 1 === u);

        if (hw) {
          let typeClass = "hw-default";
          if (hw.equipmentType === "SERVER") typeClass = "hw-server";
          if (hw.equipmentType === "SWITCH" || hw.equipmentType === "ROUTER")
            typeClass = "hw-switch";
          if (hw.equipmentType === "STORAGE") typeClass = "hw-storage";

          const powerAlert = hw.isSinglePower
            ? `<span class="single-power-alert" title="단일 전원 장비입니다">◀</span>`
            : "";

          const hwName = hw.description || hw.equipmentType;

          // 🌟 1U의 높이를 29px(border 1px 포함)로 계산하여 랙 슬롯 전체 높이 지정
          const heightCss = `height: ${29 * hw.size}px;`;

          // 🌟 15년 차의 핵심: 장비 크기만큼 연속된 U 번호 그룹(Rail) 생성
          let uNumsHTML = `<div class="u-num-group">`;
          for (let i = 0; i < hw.size; i++) {
            // 예: 12U 위치에 4U 장비면 -> 12U, 11U, 10U, 9U 순서로 생성
            uNumsHTML += `<div class="u-num">${u - i}U</div>`;
          }
          uNumsHTML += `</div>`;

          slotsHTML += `
              <div class="rack-slot" style="${heightCss}">
                  ${uNumsHTML} <div class="hw-item-wrapper"> <div class="hw-item ${typeClass}">
                          <div class="hw-info hw-name" title="${hwName}">${hwName}</div>
                          <div class="hw-info hw-model" title="${hw.model}">${hw.model}</div>
                          <div class="hw-info hw-serial" title="${hw.serialNo}">
                              ${powerAlert} ${hw.serialNo}
                          </div>
                      </div>
                  </div>
              </div>
          `;
          skipUs = hw.size - 1;
        } else {
          // 비어있는 슬롯 (1U)
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

      return `
            <div class="rack-cabinet-wrapper">
                <div class="rack-title">
                    ${rack.name} [${rack.rackNo}]
                    <button class="btn small danger" style="margin-left:10px;" onclick="window.deleteRack(${rack.id})">삭제</button>
                </div>
                <div class="rack-cabinet">
                    ${slotsHTML}
                </div>
            </div>
        `;
    })
    .join("");
}

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
