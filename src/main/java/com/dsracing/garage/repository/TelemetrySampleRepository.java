package com.dsracing.garage.repository;

import com.dsracing.garage.model.entity.TelemetrySample;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TelemetrySampleRepository extends JpaRepository<TelemetrySample, Long> {
}
