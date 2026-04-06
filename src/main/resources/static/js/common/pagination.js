export class Pagination {
  /**
   * @param {HTMLElement} containerElement - 페이징 버튼이 렌더링될 컨테이너 DOM
   * @param {Function} onPageChangeCallback - 페이지 버튼 클릭 시 실행될 콜백 함수 (pageNo 전달)
   */
  constructor(containerElement, onPageChangeCallback) {
    this.container = containerElement;
    this.onPageChangeCallback = onPageChangeCallback;
    // 한 번에 보여줄 페이지 번호 개수 (예: 1~10)
    this.pageGroupSize = 10;
  }

  /**
   * 백엔드에서 데이터를 받아온 후 페이징 UI를 다시 그립니다.
   * @param {number} totalPages - 전체 페이지 수
   * @param {number} currentIndex - 현재 선택된 페이지 인덱스 (0부터 시작)
   */
  render(totalPages, currentIndex) {
    this.container.innerHTML = ""; // 기존 버튼 초기화

    if (totalPages <= 1) return; // 1페이지 이하면 그리지 않음

    // 🌟 1. '◀ 이전' 버튼
    const prevBtn = document.createElement("button");
    prevBtn.className = "page-btn";
    prevBtn.innerText = "◀ 이전";
    prevBtn.disabled = currentIndex === 0;
    prevBtn.addEventListener("click", () =>
      this.onPageChangeCallback(currentIndex - 1)
    );
    this.container.appendChild(prevBtn);

    // 🌟 2. 10개 단위 블록 계산 (예: 현재 13페이지면 11~20만 노출)
    const currentGroup = Math.floor(currentIndex / this.pageGroupSize);
    const startPage = currentGroup * this.pageGroupSize;
    const endPage = Math.min(startPage + this.pageGroupSize, totalPages);

    // 페이지 번호 버튼 생성
    for (let i = startPage; i < endPage; i++) {
      const pageBtn = document.createElement("button");
      pageBtn.className = `page-btn ${i === currentIndex ? "active" : ""}`;
      pageBtn.innerText = i + 1; // 화면에는 1부터 표기
      pageBtn.addEventListener("click", () => {
        if (i !== currentIndex) this.onPageChangeCallback(i);
      });
      this.container.appendChild(pageBtn);
    }

    // 🌟 3. '다음 ▶' 버튼
    const nextBtn = document.createElement("button");
    nextBtn.className = "page-btn";
    nextBtn.innerText = "다음 ▶";
    nextBtn.disabled = currentIndex === totalPages - 1;
    nextBtn.addEventListener("click", () =>
      this.onPageChangeCallback(currentIndex + 1)
    );
    this.container.appendChild(nextBtn);
  }
}
