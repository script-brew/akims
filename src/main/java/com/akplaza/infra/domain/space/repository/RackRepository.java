package com.akplaza.infra.domain.space.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.akplaza.infra.domain.space.entity.Rack;

public interface RackRepository extends JpaRepository<Rack, Long>, JpaSpecificationExecutor<Rack> {

    boolean existsByRackNo(String rackNo);

    boolean existsByLocationId(Long locationId);
}