// HTML 문서가 완전히 로드된 후 스크립트 실행
document.addEventListener("DOMContentLoaded", function () {
  const registerForm = document.getElementById("serverRegisterForm");

  if (registerForm) {
    registerForm.addEventListener("submit", function (event) {
      event.preventDefault(); // 폼 기본 제출(새로고침) 방지

      // 1. 입력된 IP 주소를 배열로 변환
      const ipInput = document.getElementById("ipAddresses").value;
      const ipArray = ipInput ? ipInput.split(",").map((ip) => ip.trim()) : [];

      // 2. 백엔드 DTO(ServerCreateRequest) 구조에 맞게 JSON 객체 조립
      const requestData = {
        hardwareId: null,
        hostName: document.getElementById("hostName")?.value || "",
        category: document.getElementById("category")?.value || "WEB",
        environment: document.getElementById("environment")?.value || "PROD",
        serverType: document.getElementById("serverType")?.value || "VIRTUAL",
        os: document.getElementById("os")?.value || "",
        // 🌟 엘리먼트가 없으면 null 에러 대신 빈 문자열('')을 반환하도록 방어
        description: document.getElementById("description")?.value || "",
        spec: {
          cpuCore: parseInt(document.getElementById("cpuCore")?.value) || 0,
          memoryGb: parseInt(document.getElementById("memoryGb")?.value) || 0,
        },
        ipAddresses: ipArray,
        disks: [{ mountPoint: "/", sizeGb: 100, diskType: "SSD" }],
      };

      // 3. Fetch API를 이용해 REST API로 POST 요청 전송
      fetch("/api/v1/servers", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(requestData),
      })
        .then((response) => {
          if (response.ok) {
            alert("서버 자산이 성공적으로 등록되었습니다!");
            // 4. 모달 닫기 및 화면 새로고침
            const modal = bootstrap.Modal.getInstance(
              document.getElementById("serverRegisterModal")
            );
            modal.hide();
            window.location.reload();
          } else {
            return response.text().then((text) => {
              throw new Error(text);
            });
          }
        })
        .catch((error) => {
          console.error("Error:", error);
          alert("서버 등록에 실패했습니다.\n" + error.message);
        });
    });
  }
});
