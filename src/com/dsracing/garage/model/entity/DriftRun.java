package com.dsracing.garage.model.entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "drift_runs")
public class DriftRun {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private double score;
    private double maxAngle;
    private double avgLateralG;
    private long durationMs;

    @OneToMany(mappedBy = "driftRun", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TelemetrySample> telemetry;

    // getters y setters
}
