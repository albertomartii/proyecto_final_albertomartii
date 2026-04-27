package com.dsracing.garage.model.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "dyno_results")
public class DynoResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double maxPower;       // HP
    private double maxTorque;      // Nm
    private double time0to60;      // segundos — 0 a 60 km/h
    private double time0to100;     // segundos — 0 a 100 km/h

    @Lob
    private String powerCurveJson; // serie rpm→power

    // ── Getters y setters ─────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public double getMaxPower() { return maxPower; }
    public void setMaxPower(double maxPower) { this.maxPower = maxPower; }

    public double getMaxTorque() { return maxTorque; }
    public void setMaxTorque(double maxTorque) { this.maxTorque = maxTorque; }

    public double getTime0to60() { return time0to60; }
    public void setTime0to60(double time0to60) { this.time0to60 = time0to60; }

    public double getTime0to100() { return time0to100; }
    public void setTime0to100(double time0to100) { this.time0to100 = time0to100; }

    public String getPowerCurveJson() { return powerCurveJson; }
    public void setPowerCurveJson(String powerCurveJson) { this.powerCurveJson = powerCurveJson; }
}