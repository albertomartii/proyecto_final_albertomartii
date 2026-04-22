package com.dsracing.garage.repository;

import com.dsracing.garage.model.entity.DriftRun;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriftRunRepository extends JpaRepository<DriftRun, Long> {
}
