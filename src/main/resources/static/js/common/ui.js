/**
 * AKIMS 공통 UI 제어 모듈
 * 모달, 체크박스, 화면 렌더링 관련 반복적인 DOM 조작을 캡슐화합니다.
 */
export const ui = {
  // ==========================================
  // 1. 모달(Modal) 제어
  // ==========================================
  openModal(modalId, titleId = null, titleText = null) {
    const modal = document.getElementById(modalId);
    if (titleId && titleText) {
      const titleEl = document.getElementById(titleId);
      if (titleEl) titleEl.textContent = titleText;
    }
    if (modal) modal.style.display = "flex";
  },

  closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) modal.style.display = "none";
  },

  // ==========================================
  // 2. 체크박스(Checkbox) 다중 선택 제어
  // ==========================================
  setupCheckAll(checkAllId, itemClass) {
    const checkAllObj = document.getElementById(checkAllId);
    if (!checkAllObj) return;

    // 전체 선택/해제 이벤트
    checkAllObj.addEventListener("change", (e) => {
      document.querySelectorAll(`.${itemClass}`).forEach((cb) => {
        cb.checked = e.target.checked;
      });
    });
  },

  getCheckedIds(itemClass) {
    const checkedBoxes = document.querySelectorAll(`.${itemClass}:checked`);
    return Array.from(checkedBoxes).map((cb) => cb.getAttribute("data-id"));
  },

  clearCheckAll(checkAllId) {
    const checkAllObj = document.getElementById(checkAllId);
    if (checkAllObj) checkAllObj.checked = false;
  },

  // ==========================================
  // 3. 보안 유틸리티 (XSS 방어)
  // ==========================================
  escapeHtml(unsafe) {
    if (!unsafe) return "";
    return unsafe
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#039;");
  },
};
