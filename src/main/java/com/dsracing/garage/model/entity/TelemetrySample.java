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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getTimestampMs() {
        return timestampMs;
    }

    public void setTimestampMs(long timestampMs) {
        this.timestampMs = timestampMs;
    }

    public double getRpm() {
        return rpm;
    }

    public void setRpm(double rpm) {
        this.rpm = rpm;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public double getLateralG() {
        return lateralG;
    }

    public void setLateralG(double lateralG) {
        this.lateralG = lateralG;
    }

    public double getYawAngle() {
        return yawAngle;
    }

    public void setYawAngle(double yawAngle) {
        this.yawAngle = yawAngle;
    }

    public DriftRun getDriftRun() {
        return driftRun;
    }

    public void setDriftRun(DriftRun driftRun) {
        this.driftRun = driftRun;
    }
}
