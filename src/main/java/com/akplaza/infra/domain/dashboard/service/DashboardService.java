package com.akplaza.infra.domain.dashboard.service;

import com.akplaza.infra.domain.dashboard.dto.DashboardSummaryResponse;
import com.akplaza.infra.domain.device.repository.NetworkDeviceRepository;
import com.akplaza.infra.domain.device.repository.ServerRepository;
import com.akplaza.infra.domain.space.repository.LocationRepository;
import com.akplaza.infra.domain.space.repository.RackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final ServerRepository serverRepository;
    private final NetworkDeviceRepository networkDeviceRepository;
    private final LocationRepository locationRepository;
    private final RackRepository rackRepository;

    public DashboardSummaryResponse getSummary() {
        // OS별 통계 매핑
        Map<String, Long> osMap = serverRepository.countByOs().stream()
                .collect(Collectors.toMap(
                        row -> row[0] != null ? row[0].toString() : "UNKNOWN",
                        row -> (Long) row[1]));

        // 환경별(운영/개발 등) 통계 매핑
        Map<String, Long> envMap = serverRepository.countByEnvironment().stream()
                .collect(Collectors.toMap(
                        row -> row[0] != null ? row[0].toString() : "UNKNOWN",
                        row -> (Long) row[1]));

        return DashboardSummaryResponse.builder()
                .totalServers(serverRepository.count())
                .totalNetworkDevices(networkDeviceRepository.count())
                .totalLocations(locationRepository.count())
                .totalRacks(rackRepository.count())
                .serverByOs(osMap)
                .serverByEnv(envMap)
                .build();
    }
}