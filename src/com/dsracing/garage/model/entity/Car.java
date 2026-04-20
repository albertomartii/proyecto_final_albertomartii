package com.dsracing.garage.model.entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "cars")
public class Car implements Comparable<Car> {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String make;
    private String model;
    private int year;

    // Valores base para simulación
    private double basePower; // en HP
    private double baseTorque; // Nm
    private double mass; // kg
    private double gripBase; // coeficiente de fricción
    private double weightDistributionFront; // 0..1

    @ManyToOne
    @JoinColumn(name = "garage_id")
    private Garage garage;

    @ManyToMany
    @JoinTable(name = "car_parts",
            joinColumns = @JoinColumn(name = "car_id"),
            inverseJoinColumns = @JoinColumn(name = "part_id"))
    private List<Part> parts;

    // utilidad para orden natural
    public double getPerformanceIndex() {
        return (basePower / mass) * 1000.0;
    }

    @Override
    public int compareTo(Car other) {
        return Double.compare(other.getPerformanceIndex(), this.getPerformanceIndex()); // descendente
    }

    // getters y setters
}
