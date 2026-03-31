package com.akplaza.infra.domain.space.dto;

import com.akplaza.infra.domain.space.entity.Rack;

import lombok.Getter;

@Getter
public class RackResponse {
    private Long id;
    private Long locationId;
    private String locationName;
    private String rackNo;
    private String name;
    private Integer size;

    public RackResponse(Rack rack) {
        this.id = rack.getId();
        this.rackNo = rack.getRackNo();
        this.name = rack.getName();
        this.size = rack.getSize();
        if (rack.getLocation() != null) {
            this.locationId = rack.getLocation().getId();
            this.locationName = rack.getLocation().getName();
        }
    }
}