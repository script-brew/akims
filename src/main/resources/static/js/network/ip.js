import { api } from "../common/api.js";
import { ui } from "../common/ui.js";

import { SearchFilter } from "../common/search-filter.js";
import { Pagination } from "../common/pagination.js";

// --- DOM Elements ---
const tableBody = document.getElementById("cidr-table-body");
const mapContainer = document.getElementById("ip-map-container");
const mapTitle = document.getElementById("map-title");
const ipGrid = document.getElementById("ip-grid");

const inputPrefix = document.getElementById("cidr-prefix");
const selectMask = document.getElementById("cidr-mask");
const inputDesc = document.getElementById("cidr-desc");

const btnOpenModal = document.getElementById("btn-open-modal");
const btnCancel = document.getElementById("btn-cancel");
const btnSave = document.getElementById("btn-save");
const btnDeleteCidr = document.getElementById("btn-delete-cidr");

const btnDownloadExcel = document.getElementById("btn-download-excel");
const btnUploadExcel = document.getElementById("btn-upload-excel");
const excelFileInput = document.getElementById("excel-file-input");

let cidrList = [];

let searchFilter, pagination;
let currentPage = 0;

document.addEventListener("DOMContentLoaded", async () => {
  const filterOptions = [
    { value: "cidrBlock", label: "IP 대역 (CIDR)" },
    { value: "description", label: "설명/용도" },
  ];

  searchFilter = new SearchFilter(
    document.getElementById("ip-search-filter"),
    filterOptions,
    () => {
      currentPage = 0;
      loadCidrs();
    }
  );

  pagination = new Pagination(
    document.querySelector("#ip-pagination .pagination-container"),
    (pageNo) => {
      currentPage = pageNo;
      loadCidrs();
    }
  );

  await loadCidrs();
  setupEventListeners();
});

function setupEventListeners() {
  btnOpenModal.addEventListener("click", () => {
    // 🌟 모달 열릴 때 초기화
    inputPrefix.value = "";
    selectMask.value = "24";
    inputDesc.value = "";
    ui.openModal("cidr-modal", "modal-title", "새 IP 대역 등록"); // 타이틀 ID가 있다면 맞춰주세요
  });
  btnCancel.addEventListener("click", () => ui.closeModal("cidr-modal"));
  btnSave.addEventListener("click", saveCidr);
  btnDeleteCidr.addEventListener("click", deleteCidrs);

  btnDownloadExcel.addEventListener("click", () => {
    // 현재 검색된 필터 조건을 그대로 URL 파라미터로 가져옴
    const filterParams = searchFilter.getQueryParams();
    // 브라우저의 기본 다운로드 동작을 유도하기 위해 window.location.href 사용
    window.location.href = `/api/v1/ip-cidrs/excel/download?${filterParams.replace(
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
      const response = await fetch("/api/v1/ip-cidrs/excel/upload", {
        method: "POST",
        body: formData,
        // 주의: FormData 사용 시 Content-Type 헤더를 명시적으로 설정하지 않아야 브라우저가 boundary를 자동 생성합니다.
      });
      const result = await response.json();

      if (response.ok) {
        alert("엑셀 데이터가 성공적으로 반영되었습니다.\n" + result.message);
        loadCidrs(); // 업로드 성공 후 목록 새로고침
      } else {
        alert("엑셀 업로드 실패:\n" + result.message);
      }
    } catch (error) {
      console.error("엑셀 업로드 에러", error);
      alert("오류 발생: " + error.message);
    } finally {
      excelFileInput.value = ""; // 초기화하여 같은 파일 다시 선택 가능하게 함
    }
  });

  ui.setupCheckAll("check-all", "cidr-checkbox-item");
}

async function loadCidrs() {
  try {
    const filterParams = searchFilter.getQueryParams();
    const url = `/api/v1/ip-cidrs?page=${currentPage}&size=20${filterParams}`;
    const responseData = await api.get(url);
    cidrList = responseData.content || [];

    renderTable();
    pagination.render(responseData.totalPages, responseData.number);
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

    // 3. 🌟 서브넷 파싱 및 호스트 개수 계산
    const [baseIpStr, maskStr] = cidrBlock.split("/");
    const mask = parseInt(maskStr) || 24; // 마스크가 없으면 기본값 24 (/24)

    // IP 개수 계산 로직: 2^(32 - mask)
    // 예: /24 = 256개, /25 = 128개, /26 = 64개
    const numIps = Math.pow(2, 32 - mask);

    const ipParts = baseIpStr.split(".");
    const prefix = `${ipParts[0]}.${ipParts[1]}.${ipParts[2]}.`; // 예: "10.10.10."

    // 시작하는 마지막 자리 숫자 (예: 10.10.10.128/25 이면 128부터 시작)
    const startOctet = parseInt(ipParts[3]) || 0;
    const endOctet = startOctet + numIps - 1;

    // 4. 동적으로 계산된 시작점(startOctet)부터 끝점(endOctet)까지 네모칸 생성
    let gridHTML = "";
    for (let i = startOctet; i <= endOctet; i++) {
      const currentIp = `${prefix}${i}`;
      const assignedInfo = usedIps.find((ip) => ip.ipAddress === currentIp);

      let boxClass = "ip-box";
      let tooltipText = currentIp;

      const isIpUsed =
        assignedInfo && (assignedInfo.isUsed || assignedInfo.used);

      if (isIpUsed) {
        const targetName = assignedInfo.assignedTargetName || "알 수 없음";

        if (assignedInfo.assignedType === "SERVER") {
          boxClass += " ip-used-server";
          tooltipText = `[서버] ${targetName} \n(${currentIp})`;
        } else if (assignedInfo.assignedType === "NETWORK_DEVICE") {
          boxClass += " ip-used-network";
          tooltipText = `[네트워크] ${targetName} \n(${currentIp})`;
        } else {
          boxClass += " ip-gateway";
          tooltipText = `[예약됨/기타] \n(${currentIp})`;
        }
      } else if (i === startOctet || i === endOctet) {
        // 🌟 수정됨: 동적인 네트워크 시작 주소와 브로드캐스트 주소 예약
        boxClass += " ip-gateway";
        tooltipText =
          i === startOctet
            ? `네트워크 주소 예약 (${currentIp})`
            : `브로드캐스트 예약 (${currentIp})`;
      }

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
  const prefix = inputPrefix.value.trim();
  const mask = parseInt(selectMask.value);

  // 🚨 방어 로직 1: IP 형식 기본 검증
  const ipRegex =
    /^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/;
  if (!ipRegex.test(prefix)) {
    alert("올바른 IPv4 주소 형식이 아닙니다.\n(예: 192.168.10.0)");
    inputPrefix.focus();
    return;
  }

  // 🚨 방어 로직 2: 서브넷 마스크 기반 '네트워크 시작 주소' 정확성 검증 (비트 연산)
  const ipParts = prefix.split(".").map(Number);

  // 1) 입력받은 IP를 32비트 정수로 변환
  // 주의: 자바스크립트의 비트 연산자는 32비트 부호 있는 정수를 사용하므로 >>> 연산자로 처리해야 안전합니다.
  const ipInt =
    ((ipParts[0] << 24) |
      (ipParts[1] << 16) |
      (ipParts[2] << 8) |
      ipParts[3]) >>>
    0;

  // 2) 서브넷 마스크 생성 (예: /24 이면 왼쪽부터 24개가 1이고 나머지가 0인 비트)
  const maskInt = (0xffffffff << (32 - mask)) >>> 0;

  // 3) AND 연산을 통해 진짜 네트워크 주소 산출
  const networkInt = (ipInt & maskInt) >>> 0;

  // 4) 산출된 네트워크 주소를 다시 문자열 IP로 변환
  const expectedIp = [
    (networkInt >>> 24) & 255,
    (networkInt >>> 16) & 255,
    (networkInt >>> 8) & 255,
    networkInt & 255,
  ].join(".");

  // 5) 사용자가 입력한 IP가 실제 네트워크 시작 주소와 일치하는지 확인
  if (prefix !== expectedIp) {
    alert(
      `입력하신 IP(${prefix})는 /${mask} 대역의 올바른 네트워크 시작 주소가 아닙니다.\n\n` +
        `/${mask} 대역의 올바른 네트워크 주소는 '${expectedIp}' 입니다.\n` +
        `IP 주소를 자동으로 수정하시겠습니까?`
    );

    // 사용자가 잘못 입력했을 경우 올바른 주소로 자동 교정해주는 편의성 제공
    inputPrefix.value = expectedIp;
    return; // 저장을 멈추고 사용자가 수정된 값을 확인하게 함
  }

  // 통과 시 백엔드 규격에 맞게 문자열 병합 (예: "192.168.10.0/24")
  const requestData = {
    cidrBlock: `${prefix}/${mask}`,
    description: inputDesc.value.trim(),
  };

  try {
    await api.post("/api/v1/ip-cidrs", requestData);
    alert("IP 대역이 안전하게 등록되었습니다.");
    ui.closeModal("cidr-modal");
    loadCidrs();
  } catch (e) {
    // API 에러 처리 (예: 중복된 CIDR)
  }
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
