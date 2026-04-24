package com.dsracing.garage.util.csv;

import com.dsracing.garage.model.entity.Car;
import com.dsracing.garage.model.entity.DriftRun;
import com.dsracing.garage.model.entity.DynoResult;
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
import java.util.Map;
import java.util.TreeMap;

/**
 * Exporta resultados de Dyno y Drift a archivos CSV.
 *
 * Uso:
 *   Path file = CsvExporter.exportDyno(car, dynoResult, Path.of("exports"));
 *   Path file = CsvExporter.exportDrift(car, driftRun, Path.of("exports"));
 */
public class CsvExporter {

    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter READABLE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final ObjectMapper JSON = new ObjectMapper();

    // -------------------------------------------------------------------------
    // DYNO
    // -------------------------------------------------------------------------

    /**
     * Exporta un resultado de dinamómetro a CSV.
     *
     * Columnas: rpm, power_hp
     * Cabecera con metadatos del coche y resumen de potencia/torque.
     *
     * @param car        Coche al que pertenece el resultado
     * @param dyno       Resultado del dyno
     * @param outputDir  Directorio donde guardar el archivo (se crea si no existe)
     * @return           Path del archivo generado
     */
    public static Path exportDyno(Car car, DynoResult dyno, Path outputDir) throws IOException {
        Files.createDirectories(outputDir);

        String filename = String.format("dyno_%s_%s_%s.csv",
                sanitize(car.getMake()),
                sanitize(car.getModel()),
                LocalDateTime.now().format(TIMESTAMP_FMT));

        Path file = outputDir.resolve(filename);

        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(file, StandardCharsets.UTF_8))) {

            // --- Cabecera de metadatos ---
            pw.println("# DS Racing Garage - Resultado Dyno");
            pw.println("# Fecha," + LocalDateTime.now().format(READABLE_FMT));
            pw.println("# Coche," + car.getMake() + " " + car.getModel() + " (" + car.getYear() + ")");
            pw.println("# Max Power (HP)," + String.format("%.2f", dyno.getMaxPower()));
            pw.println("# Max Torque (Nm)," + String.format("%.2f", dyno.getMaxTorque()));
            pw.println("#");

            // --- Columnas ---
            pw.println("rpm,power_hp");

            // --- Datos de la curva de potencia ---
            Map<Integer, Double> curve = parsePowerCurve(dyno.getPowerCurveJson());
            for (Map.Entry<Integer, Double> entry : curve.entrySet()) {
                pw.printf("%d,%.2f%n", entry.getKey(), entry.getValue());
            }
        }

        System.out.println("CsvExporter: dyno exportado → " + file.toAbsolutePath());
        return file;
    }

    // -------------------------------------------------------------------------
    // DRIFT
    // -------------------------------------------------------------------------

    /**
     * Exporta un resultado de simulación de drift a CSV.
     *
     * Archivo 1 - Resumen: score, maxAngle, avgLateralG, durationMs
     * Archivo 2 - Telemetría: timestampMs, rpm, speed_ms, lateral_g, yaw_angle
     *
     * @param car        Coche al que pertenece el resultado
     * @param driftRun   Resultado de la simulación
     * @param outputDir  Directorio donde guardar los archivos
     * @return           Path del archivo de telemetría (el principal)
     */
    public static Path exportDrift(Car car, DriftRun driftRun, Path outputDir) throws IOException {
        Files.createDirectories(outputDir);

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FMT);
        String carTag = sanitize(car.getMake()) + "_" + sanitize(car.getModel());

        // --- Archivo de resumen ---
        Path summaryFile = outputDir.resolve(
                String.format("drift_summary_%s_%s.csv", carTag, timestamp));

        try (PrintWriter pw = new PrintWriter(
                Files.newBufferedWriter(summaryFile, StandardCharsets.UTF_8))) {

            pw.println("# DS Racing Garage - Resumen Drift Run");
            pw.println("# Fecha," + LocalDateTime.now().format(READABLE_FMT));
            pw.println("# Coche," + car.getMake() + " " + car.getModel() + " (" + car.getYear() + ")");
            pw.println("#");
            pw.println("campo,valor");
            pw.printf("score,%.2f%n",          driftRun.getScore());
            pw.printf("max_angle_deg,%.2f%n",   driftRun.getMaxAngle());
            pw.printf("avg_lateral_g,%.3f%n",   driftRun.getAvgLateralG());
            pw.printf("duration_ms,%d%n",        driftRun.getDurationMs());
        }

        System.out.println("CsvExporter: drift summary exportado → " + summaryFile.toAbsolutePath());

        // --- Archivo de telemetría ---
        Path telemetryFile = outputDir.resolve(
                String.format("drift_telemetry_%s_%s.csv", carTag, timestamp));

        try (PrintWriter pw = new PrintWriter(
                Files.newBufferedWriter(telemetryFile, StandardCharsets.UTF_8))) {

            pw.println("# DS Racing Garage - Telemetría Drift Run");
            pw.println("# Fecha," + LocalDateTime.now().format(READABLE_FMT));
            pw.println("# Coche," + car.getMake() + " " + car.getModel() + " (" + car.getYear() + ")");
            pw.println("#");
            pw.println("timestamp_ms,rpm,speed_ms,lateral_g,yaw_angle_deg");

            if (driftRun.getTelemetry() != null) {
                for (TelemetrySample s : driftRun.getTelemetry()) {
                    pw.printf("%d,%.1f,%.3f,%.4f,%.3f%n",
                            s.getTimestampMs(),
                            s.getRpm(),
                            s.getSpeed(),
                            s.getLateralG(),
                            s.getYawAngle());
                }
            }
        }

        System.out.println("CsvExporter: drift telemetry exportado → " + telemetryFile.toAbsolutePath());
        return telemetryFile;
    }

    // -------------------------------------------------------------------------
    // Utilidades privadas
    // -------------------------------------------------------------------------

    /**
     * Parsea el JSON de curva de potencia {"1000": 150.5, "1250": 160.2, ...}
     * y devuelve un TreeMap ordenado por RPM.
     */
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

    /** Elimina caracteres no válidos para nombres de archivo. */
    private static String sanitize(String input) {
        if (input == null) return "unknown";
        return input.replaceAll("[^a-zA-Z0-9_\\-]", "_").toLowerCase();
    }
}