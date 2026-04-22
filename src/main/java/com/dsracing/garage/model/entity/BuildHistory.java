package com.dsracing.garage.model.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "build_history")
public class BuildHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Car car;

    @ManyToOne
    private User owner;

    private Instant timestamp;

    @Lob
    private String partsSnapshotJson; // snapshot inmutable de piezas

    @Enumerated(EnumType.STRING)
    private Discipline targetDiscipline; // DRIFT, TOUGE, RALLY, ASPHALT

    @OneToOne(cascade = CascadeType.ALL)
    private DynoResult dynoResult;

    @OneToOne(cascade = CascadeType.ALL)
    private DriftRun driftRun;

    // getters y setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Car getCar() {
        return car;
    }

    public void setCar(Car car) {
        this.car = car;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getPartsSnapshotJson() {
        return partsSnapshotJson;
    }

    public void setPartsSnapshotJson(String partsSnapshotJson) {
        this.partsSnapshotJson = partsSnapshotJson;
    }

    public Discipline getTargetDiscipline() {
        return targetDiscipline;
    }

    public void setTargetDiscipline(Discipline targetDiscipline) {
        this.targetDiscipline = targetDiscipline;
    }

    public DynoResult getDynoResult() {
        return dynoResult;
    }

    public void setDynoResult(DynoResult dynoResult) {
        this.dynoResult = dynoResult;
    }

    public DriftRun getDriftRun() {
        return driftRun;
    }

    public void setDriftRun(DriftRun driftRun) {
        this.driftRun = driftRun;
    }
}
