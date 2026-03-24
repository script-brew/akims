package com.akplaza.infra.domain.hardware.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.akplaza.infra.domain.hardware.entity.Hardware;

public interface HardwareRepository extends JpaRepository<Hardware, Long> {
}