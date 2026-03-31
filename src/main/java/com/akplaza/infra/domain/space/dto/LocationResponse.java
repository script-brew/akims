package com.akplaza.infra.domain.space.dto;

import com.akplaza.infra.domain.space.entity.Location;

import lombok.Getter;

@Getter
public class LocationResponse {
    private Long id;
    private String name;

    public LocationResponse(Location loc) {
        this.id = loc.getId();
        this.name = loc.getName();
    }
}
