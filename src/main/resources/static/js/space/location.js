import { api } from "../common/api.js";
import { ui } from "../common/ui.js";

// --- DOM Elements ---
const locTableBody = document.getElementById("location-table-body");
const visualContainer = document.getElementById("visual-rack-container");
const visualTitle = document.getElementById("visual-title");
const rackGridArea = document.getElementById("rack-grid-area");

// Location Inputs
const locId = document.getElementById("loc-id");
const locName = document.getElementById("loc-name");
const locAddress = document.getElementById("loc-address");
const locContact = document.getElementById("loc-contact");
const locDesc = document.getElementById("loc-desc");

// Rack Inputs
const rackId = document.getElementById("rack-id");
const rackLocId = document.getElementById("rack-loc-id");
const rackNo = document.getElementById("rack-no");
const rackName = document.getElementById("rack-name");
const rackU = document.getElementById("rack-u");
const rackDesc = document.getElementById("rack-desc");

// --- State ---
let locationList = [];
let rackList = [];
let hardwareList = [];
let currentLocationId = null; // 🌟 현재 시각화 모드로 열려있는 장소 ID

document.addEventListener("DOMContentLoaded", async () => {
  await loadAllData();
  setupEventListeners();
});

// --- API Loaders ---
async function loadAllData() {
  try {
    const [locs, racks, hws] = await Promise.all([
      api.get("/api/v1/locations"),
      api.get("/api/v1/racks"),
      api.get("/api/v1/hardwares?size=1000"),
    ]);
    locationList = locs;
    rackList = racks;
    hardwareList = hws.content || [];
    renderLocationTable();

    // 랙 추가/삭제 후 뷰를 새로고침하기 위함
    if (currentLocationId) window.viewVisualRacks(currentLocationId);
  } catch (e) {
    locTableBody.innerHTML = `<tr><td colspan="6" style="text-align:center;color:red;">데이터 로드 실패</td></tr>`;
  }
}

// --- 🌟 1. Location Table Rendering ---
function renderLocationTable() {
  if (locationList.length === 0) {
    locTableBody.innerHTML = `<tr><td colspan="6" style="text-align:center;">등록된 장소가 없습니다.</td></tr>`;
    return;
  }

  locTableBody.innerHTML = locationList
    .map((loc) => {
      // 해당 장소에 속한 랙의 개수 계산
      const rackCount = rackList.filter((r) => r.locationId === loc.id).length;
      const safeDesc = ui.escapeHtml(loc.description || "-");

      return `
            <tr>
                <td><input type="checkbox" class="data-checkbox loc-checkbox-item" data-id="${
                  loc.id
                }"></td>
                <td><strong>${ui.escapeHtml(loc.name)}</strong></td>
                <td>${ui.escapeHtml(loc.address || "-")}</td>
                <td>${ui.escapeHtml(loc.contactInfo || "-")}</td>
                <td><span class="badge" style="background:#8e44ad;">${rackCount} 개</span></td>
                <td>
                    <button class="btn small" style="background-color: #34495e;" onclick="window.viewVisualRacks(${
                      loc.id
                    })">랙 뷰 보기 (Drill-down)</button>
                </td>
            </tr>
        `;
    })
    .join("");
}

// --- 🌟 2. Rack Visual Dashboard Rendering ---
window.viewVisualRacks = (locId) => {
  currentLocationId = locId;
  const loc = locationList.find((l) => l.id === locId);
  const locRacks = rackList.filter((r) => r.locationId === locId);

  visualTitle.textContent = `[${loc.name}] 실장된 랙 및 장비 현황`;

  if (locRacks.length === 0) {
    rackGridArea.innerHTML = `<div style="padding: 50px; text-align: center; width: 100%; color: #7f8c8d; font-size: 1.1rem;">이 장소에는 아직 등록된 랙이 없습니다.<br>우측 상단의 '+ 새 랙 추가' 버튼을 눌러주세요.</div>`;
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
                    
                    <div style="display: flex; align-items: stretch; border: 12px solid #2c3e50; border-radius: 4px; background: #fff; box-shadow: 10px 10px 20px rgba(0,0,0,0.3);">
                        <div style="width: 25px; background:rgb(255, 0, 0); display: flex; align-items: center; justify-content: center; border-right: 2px solid #1a252f;">
                            <div style="color: #f1c40f; writing-mode: vertical-rl; font-weight: bold; font-size: 0.75rem; letter-spacing: 3px;">A</div>
                        </div>

                        <div class="rack-cabinet" style="flex: 1; padding: 5px 0;">
                            ${slotsHtml}
                        </div>

                        <div style="width: 25px; background:rgb(0, 0, 255); display: flex; align-items: center; justify-content: center; border-left: 2px solid #1a252f;">
                            <div style="color: #f1c40f; writing-mode: vertical-rl; font-weight: bold; font-size: 0.75rem; letter-spacing: 3px;">B</div>
                        </div>
                    </div>
                </div>
            `;
      })
      .join("");
  }

  visualContainer.style.display = "block";
  visualContainer.scrollIntoView({ behavior: "smooth", block: "start" });
};

// 랙 슬롯 렌더링 헬퍼 함수 (기존 3단 그리드 로직)
function generateRackSlotsHTML(targetRack, rackHwList) {
  let slotsHTML = "";
  let skipUs = 0;

  for (let u = targetRack.size; u >= 1; u--) {
    if (skipUs > 0) {
      skipUs--;
      continue;
    }

    const hw = rackHwList.find((h) => h.rackPosition + h.size - 1 === u);

    if (hw) {
      let typeClass = "hw-default";
      if (hw.equipmentType === "SERVER") typeClass = "hw-server";
      if (
        hw.equipmentType === "SWITCH" ||
        hw.equipmentType === "ROUTER" ||
        hw.equipmentType === "FIREWALL"
      )
        typeClass = "hw-switch";
      if (hw.equipmentType === "STORAGE" || hw.equipmentType === "NAS")
        typeClass = "hw-storage";

      // 🌟 1. 도마(받침대)인 경우 전용 클래스 매핑
      if (hw.model === "shelf") typeClass = "hw-shelf";

      // 🌟 전원 연결 화살표 로직 추가
      let leftArrow = "";
      let rightArrow = "";
      if (hw.model !== "shelf") {
        if (hw.isSinglePower) {
          // 단일 전원: 빨간색으로 깜빡이는 화살표
          const alertStyle = "color:#e74c3c; animation: blink 1.5s infinite;";
          if (hw.powerLine === "B") {
            rightArrow = `<span style="${alertStyle}" title="B전원 단일 연결">▶</span>`;
          } else {
            // 'A' 이거나 구버전 데이터(null)일 경우 기본 좌측
            leftArrow = `<span style="${alertStyle}" title="A전원 단일 연결">◀</span>`;
          }
        } else {
          // 이중 전원: 양쪽 모두 초록색 화살표로 안정감 부여
          leftArrow = `<span style="color:#2ecc71;" title="A전원 연결됨">◀</span>`;
          rightArrow = `<span style="color:#2ecc71;" title="B전원 연결됨">▶</span>`;
        }
      }

      const hwName = ui.escapeHtml(hw.description || hw.equipmentType);
      const heightCss = `height: ${29 * hw.size}px;`;

      let uNumsHTML = `<div class="u-num-group">`;
      for (let i = 0; i < hw.size; i++) {
        uNumsHTML += `<div class="u-num">${u - i}U</div>`;
      }
      uNumsHTML += `</div>`;

      slotsHTML += `
                <div class="rack-slot" style="${heightCss}">
                    ${uNumsHTML}
                    <div class="hw-item-wrapper">
                      <div class="hw-item ${typeClass}" onclick="window.viewHardwareDetail(${hw.id})">
                          <div class="hw-leftarrow">${leftArrow}</div>
                          <div class="hw-info hw-name" title="${hwName}">${hwName}</div>
                          <div class="hw-rightarrow">${rightArrow}</div>
                      </div>
                    </div>
                </div>
            `;
      skipUs = hw.size - 1;
    } else {
      slotsHTML += `
                <div class="rack-slot empty" style="height: 29px;" onclick="window.addShelf(${targetRack.id}, ${u})" title="클릭하여 도마(받침대) 추가">
                    <div class="u-num-group"><div class="u-num">${u}U</div></div>
                    <div class="hw-item-wrapper empty-wrapper"></div>
                </div>
            `;
    }
  }
  return slotsHTML;
}

// --- 🌟 3. Event Listeners ---
function setupEventListeners() {
  // 장소(Location) 이벤트
  document
    .getElementById("btn-open-loc-modal")
    .addEventListener("click", () => {
      locId.value = "";
      locName.value = "";
      locAddress.value = "";
      locContact.value = "";
      locDesc.value = "";
      ui.openModal("loc-modal", "loc-modal-title", "장소 정보 등록");
    });
  document
    .getElementById("btn-cancel-loc")
    .addEventListener("click", () => ui.closeModal("loc-modal"));
  document
    .getElementById("btn-save-loc")
    .addEventListener("click", saveLocation);
  document.getElementById("btn-edit-loc").addEventListener("click", () => {
    const ids = ui.getCheckedIds("loc-checkbox-item");
    if (ids.length !== 1) {
      alert("수정할 장소를 1개만 선택해주세요.");
      return;
    }
    const target = locationList.find((l) => l.id === parseInt(ids[0]));
    locId.value = target.id;
    locName.value = target.name;
    locAddress.value = target.address || "";
    locContact.value = target.contactInfo || "";
    locDesc.value = target.description || "";
    ui.openModal("loc-modal", "loc-modal-title", "장소 정보 수정");
  });
  document
    .getElementById("btn-delete-loc")
    .addEventListener("click", deleteLocation);

  // 랙(Rack) 이벤트
  document.getElementById("btn-close-visual").addEventListener("click", () => {
    visualContainer.style.display = "none";
    currentLocationId = null;
  });
  document
    .getElementById("btn-open-rack-modal")
    .addEventListener("click", () => {
      rackId.value = "";
      rackLocId.value = currentLocationId;
      rackNo.value = "";
      rackName.value = "";
      rackU.value = 42;
      rackDesc.value = "";
      ui.openModal("rack-modal", "rack-modal-title", "새 랙(Rack) 추가");
    });
  document
    .getElementById("btn-cancel-rack")
    .addEventListener("click", () => ui.closeModal("rack-modal"));
  document.getElementById("btn-save-rack").addEventListener("click", saveRack);

  document
    .getElementById("btn-close-hw-detail")
    .addEventListener("click", () => ui.closeModal("hw-detail-modal"));
  ui.setupCheckAll("check-all-loc", "loc-checkbox-item");
}

// --- 🌟 4. CRUD Actions ---
async function saveLocation() {
  const data = {
    name: locName.value.trim(),
    address: locAddress.value.trim(),
    contactInfo: locContact.value.trim(),
    description: locDesc.value.trim(),
  };
  try {
    if (locId.value) {
      await api.put(`/api/v1/locations/${locId.value}`, data);
      alert("장소가 수정되었습니다.");
    } else {
      await api.post("/api/v1/locations", data);
      alert("장소가 등록되었습니다.");
    }
    ui.closeModal("loc-modal");
    ui.clearCheckAll("check-all-loc");
    loadAllData();
  } catch (e) {}
}

async function deleteLocation() {
  const ids = ui.getCheckedIds("loc-checkbox-item");
  if (ids.length === 0) {
    alert("삭제할 장소를 선택해주세요.");
    return;
  }
  if (!confirm("장소를 삭제하시겠습니까? 랙이 있으면 실패할 수 있습니다."))
    return;
  try {
    for (const id of ids) await api.delete(`/api/v1/locations/${id}`);
    alert("삭제되었습니다.");
    ui.clearCheckAll("check-all-loc");
    visualContainer.style.display = "none";
    loadAllData();
  } catch (e) {}
}

async function saveRack() {
  const data = {
    locationId: parseInt(rackLocId.value),
    rackNo: rackNo.value.trim(),
    name: rackName.value.trim(),
    size: parseInt(rackU.value),
    description: rackDesc.value.trim(),
  };
  try {
    if (rackId.value) {
      await api.put(`/api/v1/racks/${rackId.value}`, data);
      alert("랙이 수정되었습니다.");
    } else {
      await api.post("/api/v1/racks", data);
      alert("랙이 등록되었습니다.");
    }
    ui.closeModal("rack-modal");
    loadAllData(); // 완료되면 loadAllData에서 현재 열려있는 visual view를 자동으로 갱신함
  } catch (e) {}
}

window.editRack = (id) => {
  const target = rackList.find((r) => r.id === id);
  if (!target) return;
  rackId.value = target.id;
  rackLocId.value = target.locationId;
  rackNo.value = target.rackNo;
  rackName.value = target.name || "";
  rackU.value = target.size;
  rackDesc.value = target.description || "";
  ui.openModal("rack-modal", "rack-modal-title", "랙(Rack) 수정");
};

window.deleteRack = async (id) => {
  if (
    !confirm("이 랙을 삭제하시겠습니까? (하드웨어가 실장되어 있으면 삭제 불가)")
  )
    return;
  try {
    await api.delete(`/api/v1/racks/${id}`);
    alert("삭제되었습니다.");
    loadAllData();
  } catch (e) {}
};

window.viewHardwareDetail = (hwId) => {
  const hw = hardwareList.find((h) => h.id === hwId);
  if (!hw) return;

  const content = document.getElementById("hw-detail-content");
  // 테이블 형식으로 상세 정보를 구성합니다.
  content.innerHTML = `
        <div style="display: grid; grid-template-columns: 120px 1fr; gap: 10px; border: 1px solid #eee; padding: 15px; border-radius: 8px; background: #fafafa;">
            <div style="font-weight: bold; color: #666;">장비 구분</div><div>${
              hw.equipmentType
            }</div>
            <div style="font-weight: bold; color: #666;">모델명</div><div style="font-weight: bold; color: var(--primary-color);">${
              hw.model
            }</div>
            <div style="font-weight: bold; color: #666;">시리얼 번호</div><div style="font-family: monospace; background: #eee; padding: 2px 5px; border-radius: 3px;">${
              hw.serialNo
            }</div>
            <div style="font-weight: bold; color: #666;">전원 상태</div><div>${
              hw.isSinglePower
                ? '<span style="color:#e74c3c;">단일 전원</span>'
                : '<span style="color:#2ecc71;">이중화 전원</span>'
            }</div>
            <div style="font-weight: bold; color: #666;">장비 크기</div><div>${
              hw.size
            } U</div>
            <div style="font-weight: bold; color: #666;">설치 위치</div><div>${
              hw.rackPosition
            } U</div>
            <div style="font-weight: bold; color: #666;">비고/설명</div><div style="white-space: pre-wrap;">${
              hw.description || "-"
            }</div>
        </div>
    `;
  ui.openModal("hw-detail-modal");
};

window.addShelf = async (rackId, u) => {
  // 1. 확인 팝업
  if (!confirm(`해당 Slot(${u}U)에 도마(받침대)를 추가하시겠습니까?`)) {
    return;
  }

  // 2. 백엔드로 보낼 하드웨어(도마) 규격
  const payload = {
    equipmentType: "ETC",
    model: "shelf",
    serialNo: `SHELF-${rackId}-${u}`, // 겹치지 않게 임의의 시리얼 부여
    size: 1,
    isSinglePower: false,
    powerLine: "DUAL", // 도마는 전원이 없으므로 기본값
    rackId: rackId,
    rackPosition: u,
    description: "도마(받침대)",
    introductionYear: new Date().getFullYear(),
  };

  try {
    // 3. 하드웨어 생성 API 호출
    await api.post("/api/v1/hardwares", payload);

    // 4. 완료 후 데이터를 다시 불러와서 화면(랙 뷰) 부드럽게 갱신
    loadAllData();
  } catch (e) {
    alert("도마(받침대) 추가 중 오류가 발생했습니다.");
  }
};
