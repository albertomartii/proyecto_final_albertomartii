package com.dsracing.garage.config;

import com.dsracing.garage.model.entity.Discipline;
import com.dsracing.garage.model.entity.DriftRun;
import com.dsracing.garage.model.entity.DynoResult;
import com.dsracing.garage.repository.CarRepository;
import com.dsracing.garage.service.impl.DriftSimulator;
import com.dsracing.garage.service.impl.DynoService;
import com.dsracing.garage.util.csv.CsvExporter;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;

@Component
@Order(2)
public class SimulationRunner {

    private final CarRepository carRepository;
    private final DynoService dynoService;
    private final DriftSimulator driftSimulator;

    // Carpeta donde se guardan los CSV (relativa al directorio de trabajo)
    private static final Path EXPORT_DIR = Path.of("exports");

    public SimulationRunner(CarRepository carRepository,
                            DynoService dynoService,
                            DriftSimulator driftSimulator) {
        this.carRepository = carRepository;
        this.dynoService = dynoService;
        this.driftSimulator = driftSimulator;
    }

    //// @EventListener(ApplicationReadyEvent.class)
    @Transactional
    @Order(2)
    public void onApplicationReady() {
        carRepository.findAll().stream().findFirst().ifPresentOrElse(car -> {
            var parts = car.getParts();

            // --- Dyno ---
            DynoResult dyno = dynoService.runDyno(car, parts);
            System.out.println("=== DYNO RESULT ===");
            System.out.println("Coche     : " + car.getMake() + " " + car.getModel() + " (" + car.getYear() + ")");
            System.out.println("Max Power : " + String.format("%.1f", dyno.getMaxPower()) + " HP");
            System.out.println("Max Torque: " + String.format("%.1f", dyno.getMaxTorque()) + " Nm");

            try {
                Path csvDyno = CsvExporter.exportDyno(car, dyno, EXPORT_DIR);
                System.out.println("CSV Dyno  : " + csvDyno.toAbsolutePath());
            } catch (Exception e) {
                System.err.println("Error exportando CSV dyno: " + e.getMessage());
            }

            // --- Drift ---
            DriftRun run = driftSimulator.simulate(car, parts, Discipline.DRIFT, 42L);
            System.out.println("=== DRIFT RUN ===");
            System.out.println("Score     : " + String.format("%.2f", run.getScore()));
            System.out.println("Max Angle : " + String.format("%.2f", run.getMaxAngle()) + "°");
            System.out.println("Avg Lat G : " + String.format("%.3f", run.getAvgLateralG()) + " G");

            try {
                Path csvDrift = CsvExporter.exportDrift(car, run, EXPORT_DIR);
                System.out.println("CSV Drift : " + csvDrift.toAbsolutePath());
            } catch (Exception e) {
                System.err.println("Error exportando CSV drift: " + e.getMessage());
            }

        }, () -> System.out.println("SimulationRunner: no hay coches en la BD, omitiendo simulación inicial."));
    }
}