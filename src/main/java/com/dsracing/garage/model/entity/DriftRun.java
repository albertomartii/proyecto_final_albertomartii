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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public double getMaxAngle() {
        return maxAngle;
    }

    public void setMaxAngle(double maxAngle) {
        this.maxAngle = maxAngle;
    }

    public double getAvgLateralG() {
        return avgLateralG;
    }

    public void setAvgLateralG(double avgLateralG) {
        this.avgLateralG = avgLateralG;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public List<TelemetrySample> getTelemetry() {
        return telemetry;
    }

    public void setTelemetry(List<TelemetrySample> telemetry) {
        this.telemetry = telemetry;
    }
}
