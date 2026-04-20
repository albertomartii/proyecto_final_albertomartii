package com.dsracing.garage.model.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "telemetry_samples")
public class TelemetrySample {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private long timestampMs;
    private double rpm;
    private double speed; // m/s
    private double lateralG;
    private double yawAngle;

    @ManyToOne
    @JoinColumn(name = "drift_run_id")
    private DriftRun driftRun;

    // getters y setters
}
