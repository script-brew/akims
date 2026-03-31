package com.akplaza.infra;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing // 🌟 15년 차의 핵심 포인트: JPA Auditing 기능 활성화!
@SpringBootApplication
public class InfraApplication {

	public static void main(String[] args) {
		SpringApplication.run(InfraApplication.class, args);
	}

}
