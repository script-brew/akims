import { ui } from "./ui.js";

export class SearchFilter {
  /**
   * @param {HTMLElement} containerElement - fragment가 삽입된 최상위 컨테이너 요소
   * @param {Array} filterOptions - 예: [{value: 'hostName', label: '서버명'}, {value: 'os', label: '운영체제'}]
   * @param {Function} onSearchCallback - 필터가 변경될 때마다 호출될 외부 함수 (loadServers 등)
   */
  constructor(containerElement, filterOptions, onSearchCallback) {
    this.container = containerElement;
    this.filterOptions = filterOptions;
    this.onSearchCallback = onSearchCallback;
    this.activeFilters = []; // 쌓여있는 필터들 [{type, label, value}]

    // DOM 요소 찾기
    this.selectEl = this.container.querySelector(".sf-select");
    this.inputEl = this.container.querySelector(".sf-input");
    this.btnAdd = this.container.querySelector(".sf-btn-add");
    this.btnReset = this.container.querySelector(".sf-btn-reset");
    this.chipsContainer = this.container.querySelector(".sf-chips-container");

    this.init();
  }

  init() {
    // 1. 전달받은 옵션으로 Select 박스 채우기 (화면마다 다르게 주입됨)
    this.selectEl.innerHTML = this.filterOptions
      .map((opt) => `<option value="${opt.value}">${opt.label}</option>`)
      .join("");

    // 2. 이벤트 바인딩
    this.btnAdd.addEventListener("click", () => this.applyFilter());
    this.inputEl.addEventListener("keypress", (e) => {
      if (e.key === "Enter") this.applyFilter();
    });
    this.btnReset.addEventListener("click", () => this.resetFilters());
  }

  applyFilter() {
    const type = this.selectEl.value;
    const label = this.selectEl.options[this.selectEl.selectedIndex].text;
    const value = this.inputEl.value.trim();

    if (!value) return;

    // 동일한 타입의 필터가 있으면 값 덮어쓰기
    const existingIndex = this.activeFilters.findIndex((f) => f.type === type);
    if (existingIndex > -1) {
      this.activeFilters[existingIndex].value = value;
    } else {
      this.activeFilters.push({ type, label, value });
    }

    this.inputEl.value = ""; // 입력창 비우기
    this.renderChips();
    this.onSearchCallback(); // 외부 콜백 실행 (검색 요청)
  }

  removeFilter(type) {
    this.activeFilters = this.activeFilters.filter((f) => f.type !== type);
    this.renderChips();
    this.onSearchCallback(); // 지운 상태로 다시 검색
  }

  resetFilters() {
    this.activeFilters = [];
    this.inputEl.value = "";
    this.renderChips();
    this.onSearchCallback();
  }

  renderChips() {
    if (this.activeFilters.length === 0) {
      this.chipsContainer.innerHTML =
        '<span class="empty-filter-text">적용된 필터가 없습니다.</span>';
      return;
    }

    // X 버튼 클릭 이벤트를 달기 위해 문자열 조립 후 이벤트 리스너를 직접 바인딩
    this.chipsContainer.innerHTML = "";
    this.activeFilters.forEach((f) => {
      const chip = document.createElement("span");
      chip.className = "filter-chip";
      chip.innerHTML = `
                <strong>${ui.escapeHtml(f.label)}:</strong> ${ui.escapeHtml(
        f.value
      )}
                <span class="filter-chip-close" title="이 필터 지우기">✖</span>
            `;
      // 클로저를 이용해 현재 필터 타입 삭제 바인딩
      chip
        .querySelector(".filter-chip-close")
        .addEventListener("click", () => this.removeFilter(f.type));
      this.chipsContainer.appendChild(chip);
    });
  }

  /**
   * 외부(server.js 등)에서 API 호출 URL을 조립할 때 이 메서드를 호출하여 파라미터 문자열을 얻어감
   * 반환 예시: "&hostName=akpos&os=Windows"
   */
  getQueryParams() {
    let queryStr = "";
    this.activeFilters.forEach((f) => {
      queryStr += `&${f.type}=${encodeURIComponent(f.value)}`;
    });
    return queryStr;
  }
}
