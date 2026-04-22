package com.dsracing.garage.config;

import com.dsracing.garage.model.entity.Discipline;
import com.dsracing.garage.model.entity.DriftRun;
import com.dsracing.garage.model.entity.DynoResult;
import com.dsracing.garage.repository.CarRepository;
import com.dsracing.garage.service.impl.DriftSimulator;
import com.dsracing.garage.service.impl.DynoService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class SimulationRunner implements CommandLineRunner {

    private final CarRepository carRepository;
    private final DynoService dynoService;
    private final DriftSimulator driftSimulator;

    public SimulationRunner(CarRepository carRepository,
                            DynoService dynoService,
                            DriftSimulator driftSimulator) {
        this.carRepository = carRepository;
        this.dynoService = dynoService;
        this.driftSimulator = driftSimulator;
    }
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        carRepository.findAll().stream().findFirst().ifPresent(car -> {
            // dyno + drift + println
        });
    }

    @Override
    public void run(String... args) throws Exception {
        carRepository.findAll().stream().findFirst().ifPresent(car -> {
            var parts = car.getParts();

            DynoResult dyno = dynoService.runDyno(car, parts);
            System.out.println("=== DYNO RESULT ===");
            System.out.println("Car: " + car.getMake() + " " + car.getModel());
            System.out.println("Max Power: " + dyno.getMaxPower());
            System.out.println("Max Torque: " + dyno.getMaxTorque());

            DriftRun run = driftSimulator.simulate(car, parts, Discipline.DRIFT, 42L);
            System.out.println("=== DRIFT RUN ===");
            System.out.println("Score: " + run.getScore());
            System.out.println("Max Angle: " + run.getMaxAngle());
            System.out.println("Avg Lateral G: " + run.getAvgLateralG());
        });
    }
}
