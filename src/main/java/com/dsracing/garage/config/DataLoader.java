package com.dsracing.garage.config;

import com.dsracing.garage.model.entity.*;
import com.dsracing.garage.repository.*;
import com.dsracing.garage.model.entity.*;
import com.dsracing.garage.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.List;

@Component
@Order(1)
public class DataLoader implements CommandLineRunner {

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
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            if (userRepo.count() > 0) return;
        } catch (Exception e) {
            System.err.println("DataLoader: error comprobando userRepo.count(): " + e.getMessage());
            throw e;
        }
    }
    @Override
    public void run(String... args) throws Exception {
        if (userRepo.count() > 0) return;

        User u = new User();
        u.setUsername("alberto");
        u.setEmail("alberto@example.com");
        u.setRole("USER");
        userRepo.save(u);

        Garage g = new Garage();
        g.setName("Garaje Central");
        g.setLocation("Alicante");
        g.setOwner(u);
        garageRepo.save(g);

        Part turbo = new Part();
        turbo.setName("Turbo Stage 2");
        turbo.setType(PartType.TURBO);
        turbo.setHpDelta(60);
        turbo.setTorqueDelta(80);
        partRepo.save(turbo);

        Part tires = new Part();
        tires.setName("Drift Tires Soft");
        tires.setType(PartType.TIRES);
        tires.setGripDelta(0.12);
        partRepo.save(tires);

        Car car = new Car();
        car.setMake("Nissan");
        car.setModel("S13");
        car.setYear(1992);
        car.setBasePower(200);
        car.setBaseTorque(250);
        car.setMass(1200);
        car.setGripBase(1.0);
        car.setGarage(g);
        car.setParts(List.of(turbo, tires));
        carRepo.save(car);

        BuildHistory bh = new BuildHistory();
        bh.setCar(car);
        bh.setOwner(u);
        bh.setTimestamp(Instant.now());
        bh.setPartsSnapshotJson("{\"parts\":[\"Turbo Stage 2\",\"Drift Tires Soft\"]}");
        bh.setTargetDiscipline(Discipline.DRIFT);
        buildRepo.save(bh);
    }
}
