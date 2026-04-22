package com.dsracing.garage.repository;

import com.dsracing.garage.model.entity.DynoResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DynoResultRepository extends JpaRepository<DynoResult, Long> {
}
