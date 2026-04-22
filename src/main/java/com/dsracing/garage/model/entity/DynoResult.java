package com.dsracing.garage.model.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "dyno_results")
public class DynoResult {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private double maxPower; // HP
    private double maxTorque; // Nm

    @Lob
    private String powerCurveJson; // serie rpm->power

    // getters y setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public double getMaxPower() {
        return maxPower;
    }

    public void setMaxPower(double maxPower) {
        this.maxPower = maxPower;
    }

    public double getMaxTorque() {
        return maxTorque;
    }

    public void setMaxTorque(double maxTorque) {
        this.maxTorque = maxTorque;
    }

    public String getPowerCurveJson() {
        return powerCurveJson;
    }

    public void setPowerCurveJson(String powerCurveJson) {
        this.powerCurveJson = powerCurveJson;
    }
}
