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
}
