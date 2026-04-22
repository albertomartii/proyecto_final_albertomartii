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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public double getBasePower() {
        return basePower;
    }

    public void setBasePower(double basePower) {
        this.basePower = basePower;
    }

    public double getBaseTorque() {
        return baseTorque;
    }

    public void setBaseTorque(double baseTorque) {
        this.baseTorque = baseTorque;
    }

    public double getMass() {
        return mass;
    }

    public void setMass(double mass) {
        this.mass = mass;
    }

    public double getGripBase() {
        return gripBase;
    }

    public void setGripBase(double gripBase) {
        this.gripBase = gripBase;
    }

    public double getWeightDistributionFront() {
        return weightDistributionFront;
    }

    public void setWeightDistributionFront(double weightDistributionFront) {
        this.weightDistributionFront = weightDistributionFront;
    }

    public Garage getGarage() {
        return garage;
    }

    public void setGarage(Garage garage) {
        this.garage = garage;
    }

    public List<Part> getParts() {
        return parts;
    }

    public void setParts(List<Part> parts) {
        this.parts = parts;
    }
}
