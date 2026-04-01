package com.akplaza.infra.global.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * REST API(@RestController)와 분리하여,
 * 사용자에게 HTML 화면(Thymeleaf View)을 렌더링해주는 컨트롤러입니다.
 */
@Controller
public class ViewController {

    // 1. 메인 대시보드 화면
    @GetMapping({ "/", "/dashboard" })
    public String dashboard() {
        // src/main/resources/templates/index.html 파일을 찾아 렌더링합니다.
        return "index";
    }

    // 향후 도메인 화면 추가 시 여기에 라우팅을 추가합니다.
    // 예: @GetMapping("/view/locations") public String locations() { return
    // "space/location"; }

    // 🌟 추가: 장소 관리 화면 라우팅
    @GetMapping("/view/locations")
    public String locations() {
        // src/main/resources/templates/space/location.html 렌더링
        return "space/location";
    }

    // 🌟 추가: 랙 관리 화면 라우팅
    @GetMapping("/view/racks")
    public String racks() {
        return "space/rack";
    }

    // 🌟 추가: 하드웨어 관리 화면 라우팅
    @GetMapping("/view/hardwares")
    public String hardwares() {
        return "hardware/hardware";
    }

    // 🌟 추가: 서버 관리 화면 라우팅
    @GetMapping("/view/servers")
    public String servers() {
        return "device/server";
    }

    // 🌟 추가: IP 관리 화면 라우팅
    @GetMapping("/view/ips")
    public String ips() {
        return "network/ip";
    }
}