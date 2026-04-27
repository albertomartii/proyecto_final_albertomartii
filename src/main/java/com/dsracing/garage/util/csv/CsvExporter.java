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

    // ─────────────────────────────────────────────────────────────────────────
    // DYNO
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Exporta el resultado del dinamómetro a CSV.
     *
     * Cabecera incluye:
     *  - Datos del coche
     *  - Piezas instaladas
     *  - Max HP / Max Nm / RPM del pico
     *  - Tiempo 0-60 km/h y 0-100 km/h
     *
     * Cuerpo: curva completa rpm → hp
     */
    public static Path exportDyno(Car car, DynoResult dyno, Path outputDir) throws IOException {
        Files.createDirectories(outputDir);

        String filename = String.format("dyno_%s_%s_%s.csv",
                sanitize(car.getMake()),
                sanitize(car.getModel()),
                LocalDateTime.now().format(TIMESTAMP_FMT));

        Path file = outputDir.resolve(filename);

        try (PrintWriter pw = new PrintWriter(
                Files.newBufferedWriter(file, StandardCharsets.UTF_8))) {

            // ── Bloque 1: metadatos del coche ─────────────────────────────
            pw.println("# DS Racing Garage - Resultado Dyno");
            pw.println("# Fecha," + LocalDateTime.now().format(READABLE_FMT));
            pw.println("# Coche," + car.getMake() + " " + car.getModel()
                    + " (" + car.getYear() + ")");
            pw.println("# Potencia base (HP),"
                    + String.format("%.0f", car.getBasePower()));
            pw.println("# Torque base (Nm),"
                    + String.format("%.0f", car.getBaseTorque()));
            pw.println("# Masa base (kg),"
                    + String.format("%.0f", car.getMass()));
            pw.println("#");

            // ── Bloque 2: piezas instaladas ───────────────────────────────
            List<Part> parts = car.getParts();
            if (parts != null && !parts.isEmpty()) {
                pw.println("# --- MODIFICACIONES ---");
                for (Part p : parts) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("# ").append(p.getType().name())
                            .append(",").append(p.getName());
                    if (p.getHpDelta()    != 0) sb.append(",+").append(String.format("%.0f", p.getHpDelta())).append(" HP");
                    if (p.getTorqueDelta() != 0) sb.append(",+").append(String.format("%.0f", p.getTorqueDelta())).append(" Nm");
                    if (p.getWeightDelta() != 0) sb.append(",").append(String.format("%.0f", p.getWeightDelta())).append(" kg");
                    pw.println(sb);
                }
            } else {
                pw.println("# Modificaciones,Ninguna (stock)");
            }
            pw.println("#");

            // ── Bloque 3: resultados de la prueba ─────────────────────────
            pw.println("# --- RESULTADOS ---");
            pw.println("# Max Potencia (HP),"
                    + String.format("%.1f", dyno.getMaxPower()));
            pw.println("# Max Torque (Nm),"
                    + String.format("%.1f", dyno.getMaxTorque()));

            // RPM del pico de potencia
            Map<Integer, Double> curve = parsePowerCurve(dyno.getPowerCurveJson());
            int peakRpm = curve.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(0);
            pw.println("# RPM del pico de potencia," + peakRpm);

            // Tiempos de aceleración
            if (dyno.getTime0to60() > 0) {
                pw.println("# 0-60 km/h (s),"
                        + String.format("%.2f", dyno.getTime0to60()));
            } else {
                pw.println("# 0-60 km/h (s),N/A");
            }
            if (dyno.getTime0to100() > 0) {
                pw.println("# 0-100 km/h (s),"
                        + String.format("%.2f", dyno.getTime0to100()));
            } else {
                pw.println("# 0-100 km/h (s),N/A");
            }
            pw.println("#");

            // ── Curva de potencia ─────────────────────────────────────────
            pw.println("rpm,power_hp");
            for (Map.Entry<Integer, Double> entry : curve.entrySet()) {
                pw.printf("%d,%.2f%n", entry.getKey(), entry.getValue());
            }
        }

        System.out.println("CsvExporter: dyno exportado → " + file.toAbsolutePath());
        return file;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DRIFT
    // ─────────────────────────────────────────────────────────────────────────

    public static Path exportDrift(Car car, DriftRun driftRun, Path outputDir) throws IOException {
        Files.createDirectories(outputDir);

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FMT);
        String carTag    = sanitize(car.getMake()) + "_" + sanitize(car.getModel());

        // Resumen
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
            pw.printf("duration_ms,%d%n",     driftRun.getDurationMs());
        }

        // Telemetría
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

    // ─────────────────────────────────────────────────────────────────────────
    // Utilidades privadas
    // ─────────────────────────────────────────────────────────────────────────

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