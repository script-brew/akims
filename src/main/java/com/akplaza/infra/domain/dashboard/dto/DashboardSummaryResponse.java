package com.akplaza.infra.domain.dashboard.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class DashboardSummaryResponse {
    private long totalServers;
    private long totalNetworkDevices;
    private long totalLocations;
    private long totalRacks;

    // 차트를 그리기 위한 그룹핑 데이터
    private Map<String, Long> serverByOs;
    private Map<String, Long> serverByEnv;
}