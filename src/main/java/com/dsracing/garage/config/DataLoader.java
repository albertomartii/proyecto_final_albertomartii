package com.dsracing.garage.config;

import com.dsracing.garage.model.entity.*;
import com.dsracing.garage.repository.*;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@Order(1)
public class DataLoader {

    private final UserRepository userRepo;
    private final GarageRepository garageRepo;
    private final CarRepository carRepo;
    private final PartRepository partRepo;
    private final BuildHistoryRepository buildRepo;

    public DataLoader(UserRepository userRepo, GarageRepository garageRepo,
                      CarRepository carRepo, PartRepository partRepo,
                      BuildHistoryRepository buildRepo) {
        this.userRepo = userRepo;
        this.garageRepo = garageRepo;
        this.carRepo = carRepo;
        this.partRepo = partRepo;
        this.buildRepo = buildRepo;
    }

    /**
     * Se ejecuta UNA SOLA VEZ cuando el contexto de Spring está 100% listo.
     * @Transactional garantiza que todos los saves se confirman antes de que
     * SimulationRunner (Order 2) intente leer los datos.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    @Order(1)
    public void onApplicationReady() {
        // Guarda idempotente: si ya hay datos no hace nada
        if (userRepo.count() > 0) {
            System.out.println("DataLoader: datos ya existentes, omitiendo carga inicial.");
            return;
        }

        System.out.println("DataLoader: cargando datos iniciales...");

        // --- Usuario ---
        User u = new User();
        u.setUsername("alberto");
        u.setPassword("1234");
        u.setEmail("alberto@example.com");
        u.setRole("USER");
        userRepo.save(u);
        // --- Garaje ---
        Garage g = new Garage();
        g.setName("Garaje Central");
        g.setLocation("Alicante");
        g.setOwner(u);
        garageRepo.save(g);

        // --- Piezas ---
        Part turbo = new Part();
        turbo.setName("Turbo Stage 2");
        turbo.setType(PartType.TURBO);
        turbo.setHpDelta(60);
        turbo.setTorqueDelta(80);
        turbo.setWeightDelta(15);     // el turbo pesa algo
        turbo.setGripDelta(0);
        turbo.setSuspensionStiffnessDelta(0);
        partRepo.save(turbo);

        Part tires = new Part();
        tires.setName("Drift Tires Soft");
        tires.setType(PartType.TIRES);
        tires.setHpDelta(0);
        tires.setTorqueDelta(0);
        tires.setWeightDelta(0);
        tires.setGripDelta(0.12);
        tires.setSuspensionStiffnessDelta(0);
        partRepo.save(tires);

        // --- Coche ---
        Car car = new Car();
        car.setMake("Nissan");
        car.setModel("S13");
        car.setYear(1992);
        car.setBasePower(200);
        car.setBaseTorque(250);
        car.setMass(1200);
        car.setGripBase(1.0);
        car.setWeightDistributionFront(0.5);
        car.setGarage(g);
        car.setParts(List.of(turbo, tires));
        carRepo.save(car);

        // --- Historial de build ---
        BuildHistory bh = new BuildHistory();
        bh.setCar(car);
        bh.setOwner(u);
        bh.setTimestamp(Instant.now());
        bh.setPartsSnapshotJson("{\"parts\":[\"Turbo Stage 2\",\"Drift Tires Soft\"]}");
        bh.setTargetDiscipline(Discipline.DRIFT);
        buildRepo.save(bh);

        System.out.println("DataLoader: datos iniciales cargados correctamente.");
    }
}