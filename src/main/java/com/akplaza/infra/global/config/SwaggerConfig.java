package com.akplaza.infra.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI infraManagerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AK Plaza 인프라 자산 관리 솔루션 (AKIMS) API")
                        .description("데이터센터, 랙, 하드웨어 및 서버(물리/가상) 통합 관리 웹 API 명세서입니다.")
                        .version("v1.0.0"));
    }
}