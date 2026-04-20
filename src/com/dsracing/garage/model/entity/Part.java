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
}
