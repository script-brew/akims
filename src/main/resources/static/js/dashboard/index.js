import { api } from "../common/api.js";

document.addEventListener("DOMContentLoaded", async () => {
  try {
    // 백엔드 통계 API 호출
    const data = await api.get("/api/v1/dashboard/summary");

    // 1. 상단 요약 카드 숫자 애니메이션(혹은 즉시 바인딩) 처리
    document.getElementById("tot-server").textContent = data.totalServers;
    document.getElementById("tot-network").textContent =
      data.totalNetworkDevices;
    document.getElementById("tot-location").textContent = data.totalLocations;
    document.getElementById("tot-rack").textContent = data.totalRacks;

    // 2. 차트 렌더링 호출
    renderEnvChart(data.serverByEnv);
    renderOsChart(data.serverByOs);
  } catch (e) {
    console.error("대시보드 데이터를 불러오지 못했습니다.", e);
  }
});

// 도넛 차트: 운영 환경 분포 (PRD vs DEV)
function renderEnvChart(envData) {
  const ctx = document.getElementById("envChart").getContext("2d");

  // 환경 코드를 예쁜 한글로 변환
  const labels = Object.keys(envData).map((k) =>
    k === "PRD" ? "운영 (PRD)" : k === "DEV" ? "개발/검증 (DEV)" : k
  );
  const data = Object.values(envData);

  new Chart(ctx, {
    type: "doughnut",
    data: {
      labels: labels,
      datasets: [
        {
          data: data,
          backgroundColor: ["#e74c3c", "#2ecc71", "#f1c40f", "#9b59b6"],
          borderWidth: 2,
          borderColor: "#ffffff",
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { position: "bottom" },
      },
      cutout: "60%", // 도넛 두께 조절
    },
  });
}

// 막대 차트: 운영체제(OS) 분포
function renderOsChart(osData) {
  const ctx = document.getElementById("osChart").getContext("2d");

  const labels = Object.keys(osData);
  const data = Object.values(osData);

  new Chart(ctx, {
    type: "bar",
    data: {
      labels: labels,
      datasets: [
        {
          label: "설치된 서버 수 (대)",
          data: data,
          backgroundColor: "#3498db",
          borderRadius: 4,
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false }, // 바 차트는 항목이 하나라 범례 숨김
      },
      scales: {
        y: {
          beginAtZero: true,
          ticks: { stepSize: 1 }, // 서버 대수이므로 정수 단위로 표시
        },
      },
    },
  });
}
