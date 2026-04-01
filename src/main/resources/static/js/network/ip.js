import { api } from "../common/api.js";
import { ui } from "../common/ui.js";

// --- DOM Elements ---
const tableBody = document.getElementById("cidr-table-body");
const mapContainer = document.getElementById("ip-map-container");
const mapTitle = document.getElementById("map-title");
const ipGrid = document.getElementById("ip-grid");

const inputBlock = document.getElementById("cidr-block");
const inputDesc = document.getElementById("cidr-desc");

const btnOpenModal = document.getElementById("btn-open-modal");
const btnCancel = document.getElementById("btn-cancel");
const btnSave = document.getElementById("btn-save");
const btnDeleteCidr = document.getElementById("btn-delete-cidr");

let cidrList = [];

document.addEventListener("DOMContentLoaded", async () => {
  await loadCidrs();
  setupEventListeners();
});

function setupEventListeners() {
  btnOpenModal.addEventListener("click", () => {
    inputBlock.value = "";
    inputDesc.value = "";
    ui.openModal("cidr-modal");
  });
  btnCancel.addEventListener("click", () => ui.closeModal("cidr-modal"));
  btnSave.addEventListener("click", saveCidr);
  btnDeleteCidr.addEventListener("click", deleteCidrs);

  ui.setupCheckAll("check-all", "cidr-checkbox-item");
}

async function loadCidrs() {
  try {
    cidrList = await api.get("/api/v1/ip-cidrs");
    renderTable();
  } catch (e) {
    tableBody.innerHTML = `<tr><td colspan="4" style="text-align:center;color:red;">데이터 로드 실패</td></tr>`;
  }
}

function renderTable() {
  if (cidrList.length === 0) {
    tableBody.innerHTML = `<tr><td colspan="4" style="text-align:center;">등록된 IP 대역이 없습니다.</td></tr>`;
    return;
  }

  tableBody.innerHTML = cidrList
    .map(
      (cidr) => `
        <tr>
            <td><input type="checkbox" class="data-checkbox cidr-checkbox-item" data-id="${
              cidr.id
            }"></td>
            <td><strong>${ui.escapeHtml(cidr.cidrBlock)}</strong></td>
            <td>${ui.escapeHtml(cidr.description || "-")}</td>
            <td>
                <button class="btn small" onclick="window.viewIpMap(${
                  cidr.id
                }, '${cidr.cidrBlock}')">맵 보기</button>
            </td>
        </tr>
    `
    )
    .join("");
}

// ==========================================
// 🌟 15년 차의 핵심: Visual IP Map 렌더링 로직
// ==========================================
window.viewIpMap = async (cidrId, cidrBlock) => {
  try {
    // 1. 백엔드에서 해당 대역의 사용 중인 IP 상세 목록을 가져옵니다.
    const cidrDetail = await api.get(`/api/v1/ip-cidrs/${cidrId}`);
    const usedIps = cidrDetail.ips || []; // 사용 중인 IP 배열

    // 2. 화면 타이틀 변경 및 컨테이너 표시
    mapTitle.textContent = `Visual IP Map: ${cidrBlock} (${
      cidrDetail.description || ""
    })`;
    mapContainer.style.display = "block";
    ipGrid.innerHTML = ""; // 초기화

    // 3. /24 대역 기준으로 베이스 IP 파싱 (예: "10.10.10.0/24" -> "10.10.10.")
    const baseIpStr = cidrBlock.split("/")[0];
    const ipParts = baseIpStr.split(".");
    const prefix = `${ipParts[0]}.${ipParts[1]}.${ipParts[2]}.`; // "10.10.10."

    // 4. 0번부터 255번까지 256개의 네모칸(그리드) 생성
    let gridHTML = "";
    for (let i = 0; i <= 255; i++) {
      const currentIp = `${prefix}${i}`;

      // 현재 IP가 백엔드에서 받은 사용 중인 IP 목록에 있는지 찾기
      const assignedInfo = usedIps.find((ip) => ip.ipAddress === currentIp);

      let boxClass = "ip-box";
      let tooltipText = currentIp; // 기본 툴팁은 IP 주소만

      if (assignedInfo && assignedInfo.isUsed) {
        // 사용 중인 IP면 용도에 따라 색상 클래스 부여
        if (assignedInfo.assignedType === "SERVER") {
          boxClass += " ip-used-server";
          tooltipText = `[서버] ${assignedInfo.assignedTargetName} \n(${currentIp})`;
        } else if (assignedInfo.assignedType === "NETWORK_DEVICE") {
          boxClass += " ip-used-network";
          tooltipText = `[네트워크] ${assignedInfo.assignedTargetName} \n(${currentIp})`;
        } else {
          boxClass += " ip-gateway";
          tooltipText = `[예약됨/기타] \n(${currentIp})`;
        }
      } else if (i === 0 || i === 255) {
        // 네트워크 식별자(0) 및 브로드캐스트(255) 예약 처리
        boxClass += " ip-gateway";
        tooltipText =
          i === 0
            ? `네트워크 주소 예약 (${currentIp})`
            : `브로드캐스트 예약 (${currentIp})`;
      }

      // HTML 생성 (툴팁 데이터 바인딩)
      gridHTML += `<div class="${boxClass}" data-tooltip="${tooltipText}">${i}</div>`;
    }

    ipGrid.innerHTML = gridHTML;

    // 부드럽게 스크롤 이동
    mapContainer.scrollIntoView({ behavior: "smooth", block: "start" });
  } catch (e) {
    console.error("IP 맵 로드 실패", e);
  }
};

// --- 등록 / 삭제 로직 ---
async function saveCidr() {
  const requestData = {
    cidrBlock: inputBlock.value.trim(),
    description: inputDesc.value.trim(),
  };
  try {
    await api.post("/api/v1/ip-cidrs", requestData);
    alert("대역이 등록되었습니다.");
    ui.closeModal("cidr-modal");
    loadCidrs();
  } catch (e) {}
}

async function deleteCidrs() {
  const ids = ui.getCheckedIds("cidr-checkbox-item");
  if (ids.length === 0) {
    alert("삭제할 대역을 선택해주세요.");
    return;
  }
  if (
    !confirm(
      "선택한 대역을 삭제하시겠습니까?\n사용 중인 IP가 있으면 실패합니다."
    )
  )
    return;

  try {
    for (const id of ids) await api.delete(`/api/v1/ip-cidrs/${id}`);
    alert("삭제되었습니다.");
    loadCidrs();
    ui.clearCheckAll("check-all");
    mapContainer.style.display = "none"; // 맵 숨기기
  } catch (e) {}
}
