package com.akplaza.infra.domain.space.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LocationRequest {
    @NotBlank(message = "장소명은 필수 입력값입니다.")
    private String name;
}
