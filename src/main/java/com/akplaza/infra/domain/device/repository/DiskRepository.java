package com.akplaza.infra.domain.device.repository;

import com.akplaza.infra.domain.device.entity.Disk;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiskRepository extends JpaRepository<Disk, Long> {
}
