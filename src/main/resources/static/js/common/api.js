/**
 * AKIMS 공통 Fetch API 모듈
 * 백엔드 REST API와의 통신 및 에러 처리를 전담합니다.
 */
export const api = {
  // GET 요청
  async get(url) {
    const response = await fetch(url, {
      method: "GET",
      headers: {
        Accept: "application/json",
      },
    });
    return this.handleResponse(response);
  },

  // POST 요청 (생성)
  async post(url, data) {
    const response = await fetch(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Accept: "application/json",
      },
      body: JSON.stringify(data),
    });
    return this.handleResponse(response);
  },

  // PUT 요청 (수정)
  async put(url, data) {
    const response = await fetch(url, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
        Accept: "application/json",
      },
      body: JSON.stringify(data),
    });
    return this.handleResponse(response);
  },

  // DELETE 요청 (삭제)
  async delete(url) {
    const response = await fetch(url, {
      method: "DELETE",
      headers: {
        Accept: "application/json",
      },
    });
    return this.handleResponse(response);
  },

  // 🌟 15년 차의 핵심: 공통 응답 및 에러 처리 로직
  async handleResponse(response) {
    // 1. 정상 응답이 아닐 경우 (4xx, 5xx 에러)
    if (!response.ok) {
      let errorData;
      try {
        // 백엔드(GlobalExceptionHandler)가 내려준 ErrorResponse JSON 파싱
        errorData = await response.json();
      } catch (e) {
        // JSON 파싱 실패 시 (서버가 죽었거나 네트워크 에러 등)
        errorData = {
          message: "서버와 통신 중 알 수 없는 오류가 발생했습니다.",
        };
      }

      // 💡 DTO Validation 에러 처리 (400 Bad Request)
      if (
        response.status === 400 &&
        errorData.fieldErrors &&
        errorData.fieldErrors.length > 0
      ) {
        const errorMessages = errorData.fieldErrors
          .map((err) => `- ${err.field}: ${err.reason}`)
          .join("\n");
        alert(`[입력값 확인 부탁드립니다]\n${errorMessages}`);
      }
      // 💡 비즈니스 로직 에러 처리 (404 Not Found, 409 Conflict 등)
      else {
        alert(`[시스템 알림]\n${errorData.message}`);
      }

      // 호출한 화면(도메인 JS) 쪽으로 에러를 던져서 후속 로직(예: 모달창 닫기 등)을 중단시킴
      throw new Error(errorData.message);
    }
    const text = await response.text(); // 먼저 응답을 단순 텍스트로 읽어옵니다.

    if (!text) {
      return null; // 데이터가 텅 비어있다면 (200 OK empty body 또는 204 No Content), 에러 없이 null을 반환!
    }

    return JSON.parse(text); // 데이터가 존재할 때만 JSON 객체로 변환하여 반환!
  },
};
