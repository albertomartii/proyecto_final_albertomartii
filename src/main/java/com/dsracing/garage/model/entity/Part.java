package com.dsracing.garage.model.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "parts")
public class Part {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;

    @Enumerated(EnumType.STRING)
    private PartType type;

    // modificadores aplicables en simulación
    private double hpDelta;
    private double torqueDelta;
    private double weightDelta;
    private double gripDelta;
    private double suspensionStiffnessDelta;

    // getters y setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PartType getType() {
        return type;
    }

    public void setType(PartType type) {
        this.type = type;
    }

    public double getHpDelta() {
        return hpDelta;
    }

    public void setHpDelta(double hpDelta) {
        this.hpDelta = hpDelta;
    }

    public double getTorqueDelta() {
        return torqueDelta;
    }

    public void setTorqueDelta(double torqueDelta) {
        this.torqueDelta = torqueDelta;
    }

    public double getWeightDelta() {
        return weightDelta;
    }

    public void setWeightDelta(double weightDelta) {
        this.weightDelta = weightDelta;
    }

    public double getGripDelta() {
        return gripDelta;
    }

    public void setGripDelta(double gripDelta) {
        this.gripDelta = gripDelta;
    }

    public double getSuspensionStiffnessDelta() {
        return suspensionStiffnessDelta;
    }

    public void setSuspensionStiffnessDelta(double suspensionStiffnessDelta) {
        this.suspensionStiffnessDelta = suspensionStiffnessDelta;
    }
}
