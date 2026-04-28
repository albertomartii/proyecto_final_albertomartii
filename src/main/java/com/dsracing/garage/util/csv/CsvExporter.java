package com.dsracing.garage.util.csv;

import com.dsracing.garage.model.entity.Car;
import com.dsracing.garage.model.entity.DriftRun;
import com.dsracing.garage.model.entity.DynoResult;
import com.dsracing.garage.model.entity.Part;
import com.dsracing.garage.model.entity.TelemetrySample;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class CsvExporter {

    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter READABLE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final ObjectMapper JSON = new ObjectMapper();

    // -------------------------------------------------------------------------
    // DYNO — incluye comparativa serie vs modificado
    // -------------------------------------------------------------------------

    /**
     * Exporta el resultado del dyno con:
     *  - Metadatos del coche
     *  - Comparativa serie vs modificado (HP, Nm, masa, grip)
     *  - Piezas instaladas con sus deltas
     *  - Curva completa RPM → HP, Nm
     */
    public static Path exportDyno(Car car, DynoResult dyno,
                                  Map<Integer, Double> powerCurve,
                                  Map<Integer, Double> torqueCurve,
                                  Path outputDir) throws IOException {
        Files.createDirectories(outputDir);

        String filename = String.format("dyno_%s_%s_%s.csv",
                sanitize(car.getMake()),
                sanitize(car.getModel()),
                LocalDateTime.now().format(TIMESTAMP_FMT));

        Path file = outputDir.resolve(filename);

        // Calcular totales con piezas
        List<Part> parts = car.getParts();
        double totalHpDelta     = 0;
        double totalTorqueDelta = 0;
        double totalWeightDelta = 0;
        double totalGripDelta   = 0;
        if (parts != null) {
            for (Part p : parts) {
                totalHpDelta     += p.getHpDelta();
                totalTorqueDelta += p.getTorqueDelta();
                totalWeightDelta += p.getWeightDelta();
                totalGripDelta   += p.getGripDelta();
            }
        }

        double basePower  = car.getBasePower();
        double baseTorque = car.getBaseTorque();
        double baseMass   = car.getMass();
        double baseGrip   = car.getGripBase();

        double modPower   = basePower  + totalHpDelta;
        double modTorque  = baseTorque + totalTorqueDelta;
        double modMass    = baseMass   + totalWeightDelta;
        double modGrip    = baseGrip   + totalGripDelta;

        try (PrintWriter pw = new PrintWriter(
                Files.newBufferedWriter(file, StandardCharsets.UTF_8))) {

            // ── Cabecera general ─────────────────────────────────────────
            pw.println("# DS Racing Garage - Resultado Dyno");
            pw.println("# Fecha," + LocalDateTime.now().format(READABLE_FMT));
            pw.println("# Coche," + car.getMake() + " " + car.getModel()
                    + " (" + car.getYear() + ")");
            pw.println("#");

            // ── Comparativa serie vs modificado ──────────────────────────
            pw.println("# --- COMPARATIVA SERIE VS MODIFICADO ---");
            pw.println("# campo,serie,modificado,delta");
            pw.printf("# Potencia (HP), %.1f, %.1f, %+.1f%n",
                    basePower, modPower, totalHpDelta);
            pw.printf("# Torque (Nm), %.1f, %.1f, %+.1f%n",
                    baseTorque, modTorque, totalTorqueDelta);
            pw.printf("# Masa (kg), %.1f, %.1f, %+.1f%n",
                    baseMass, modMass, totalWeightDelta);
            pw.printf("# Grip (coef), %.3f, %.3f, %+.3f%n",
                    baseGrip, modGrip, totalGripDelta);
            pw.println("#");

            // ── Piezas instaladas ─────────────────────────────────────────
            pw.println("# --- PIEZAS INSTALADAS ---");
            if (parts == null || parts.isEmpty()) {
                pw.println("# (ninguna — coche de serie)");
            } else {
                pw.println("# nombre,tipo,hp_delta,torque_delta,peso_delta,grip_delta");
                for (Part p : parts) {
                    pw.printf("# %s,%s,%+.0f,%+.0f,%+.0f,%+.3f%n",
                            p.getName(),
                            p.getType().name(),
                            p.getHpDelta(),
                            p.getTorqueDelta(),
                            p.getWeightDelta(),
                            p.getGripDelta());
                }
            }
            pw.println("#");

            // ── Resumen del pico ──────────────────────────────────────────
            pw.println("# --- RESULTADOS PICO ---");
            pw.printf("# Max Power (HP),%.2f%n", dyno.getMaxPower());
            pw.printf("# Max Torque (Nm),%.2f%n", dyno.getMaxTorque());
            pw.println("#");

            // ── Curva completa RPM → HP y Nm ─────────────────────────────
            pw.println("rpm,power_hp,torque_nm");
            Map<Integer, Double> sortedPower  = new TreeMap<>(
                    powerCurve  != null ? powerCurve  : new TreeMap<>());
            Map<Integer, Double> sortedTorque = new TreeMap<>(
                    torqueCurve != null ? torqueCurve : new TreeMap<>());

            for (Integer rpm : sortedPower.keySet()) {
                double hp = sortedPower.getOrDefault(rpm, 0.0);
                double nm = sortedTorque.getOrDefault(rpm, 0.0);
                pw.printf("%d,%.2f,%.2f%n", rpm, hp, nm);
            }
        }

        System.out.println("CsvExporter: dyno exportado → " + file.toAbsolutePath());
        return file;
    }

    /**
     * Sobrecarga de compatibilidad — sin curvas detalladas (usa powerCurveJson).
     * Se mantiene para no romper llamadas existentes.
     */
    public static Path exportDyno(Car car, DynoResult dyno, Path outputDir) throws IOException {
        return exportDyno(car, dyno, parsePowerCurve(dyno.getPowerCurveJson()), null, outputDir);
    }

    // -------------------------------------------------------------------------
    // DRIFT
    // -------------------------------------------------------------------------

    public static Path exportDrift(Car car, DriftRun driftRun, Path outputDir) throws IOException {
        Files.createDirectories(outputDir);

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FMT);
        String carTag    = sanitize(car.getMake()) + "_" + sanitize(car.getModel());

        // --- Resumen ---
        Path summaryFile = outputDir.resolve(
                String.format("drift_summary_%s_%s.csv", carTag, timestamp));

        try (PrintWriter pw = new PrintWriter(
                Files.newBufferedWriter(summaryFile, StandardCharsets.UTF_8))) {
            pw.println("# DS Racing Garage - Resumen Drift Run");
            pw.println("# Fecha," + LocalDateTime.now().format(READABLE_FMT));
            pw.println("# Coche," + car.getMake() + " " + car.getModel()
                    + " (" + car.getYear() + ")");
            pw.println("#");
            pw.println("campo,valor");
            pw.printf("score,%.2f%n",        driftRun.getScore());
            pw.printf("max_angle_deg,%.2f%n", driftRun.getMaxAngle());
            pw.printf("avg_lateral_g,%.3f%n", driftRun.getAvgLateralG());
            pw.printf("duration_ms,%d%n",      driftRun.getDurationMs());
        }

        // --- Telemetría ---
        Path telemetryFile = outputDir.resolve(
                String.format("drift_telemetry_%s_%s.csv", carTag, timestamp));

        try (PrintWriter pw = new PrintWriter(
                Files.newBufferedWriter(telemetryFile, StandardCharsets.UTF_8))) {
            pw.println("# DS Racing Garage - Telemetría Drift Run");
            pw.println("# Fecha," + LocalDateTime.now().format(READABLE_FMT));
            pw.println("# Coche," + car.getMake() + " " + car.getModel()
                    + " (" + car.getYear() + ")");
            pw.println("#");
            pw.println("timestamp_ms,rpm,speed_ms,lateral_g,yaw_angle_deg");
            if (driftRun.getTelemetry() != null) {
                for (TelemetrySample s : driftRun.getTelemetry()) {
                    pw.printf("%d,%.1f,%.3f,%.4f,%.3f%n",
                            s.getTimestampMs(), s.getRpm(), s.getSpeed(),
                            s.getLateralG(), s.getYawAngle());
                }
            }
        }

        System.out.println("CsvExporter: drift exportado → " + telemetryFile.toAbsolutePath());
        return telemetryFile;
    }

    // -------------------------------------------------------------------------
    // Utilidades privadas
    // -------------------------------------------------------------------------

    private static Map<Integer, Double> parsePowerCurve(String json) {
        if (json == null || json.isBlank()) return new TreeMap<>();
        try {
            Map<String, Double> raw = JSON.readValue(json,
                    new TypeReference<Map<String, Double>>() {});
            Map<Integer, Double> sorted = new TreeMap<>();
            raw.forEach((k, v) -> sorted.put(Integer.parseInt(k), v));
            return sorted;
        } catch (Exception e) {
            System.err.println("CsvExporter: error parseando powerCurveJson: " + e.getMessage());
            return new TreeMap<>();
        }
    }

    private static String sanitize(String input) {
        if (input == null) return "unknown";
        return input.replaceAll("[^a-zA-Z0-9_\\-]", "_").toLowerCase();
    }
}