package com.akplaza.infra.domain.space.dto;

import com.akplaza.infra.domain.space.entity.RackType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RackRequest {

    @NotNull(message = "소속될 장소(Location) ID는 필수입니다.")
    private Long locationId;

    @NotBlank(message = "랙 번호는 필수 입력값입니다.")
    private String rackNo;

    @NotBlank(message = "랙 이름은 필수 입력값입니다.")
    private String name;

    @NotNull(message = "랙의 최대 수용 크기(U)를 입력해주세요.")
    @Min(value = 1, message = "랙 크기는 1U 이상이어야 합니다.")
    private Integer size;

    private String description;

    @NotNull(message = "랙의 타입을 선택해주세요.")
    private RackType rackType;
}
