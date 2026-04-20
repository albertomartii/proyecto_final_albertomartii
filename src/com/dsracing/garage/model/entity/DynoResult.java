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
}
