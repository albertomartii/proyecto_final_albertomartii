package com.dsracing.garage.service.impl;

import com.dsracing.garage.model.entity.Car;
import com.dsracing.garage.model.entity.DynoResult;
import com.dsracing.garage.model.entity.Part;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class DynoService {

    private static final int RPM_MIN  = 1000;
    private static final int RPM_MAX  = 8000;
    private static final int RPM_STEP = 250;

    public DynoResult runDyno(Car car, List<Part> parts) {
        double maxPower  = car.getBasePower();
        double maxTorque = car.getBaseTorque();

        if (parts != null) {
            for (Part p : parts) {
                maxPower  += p.getHpDelta();
                maxTorque += p.getTorqueDelta();
            }
        }

        Map<Integer, Double> powerCurve  = new TreeMap<>();
        Map<Integer, Double> torqueCurve = new TreeMap<>();

        // Detectar si tiene turbo (afecta el perfil de la curva)
        boolean hasTurbo = parts != null && parts.stream()
                .anyMatch(p -> p.getType() != null &&
                        p.getType().name().equals("TURBO"));

        for (int rpm = RPM_MIN; rpm <= RPM_MAX; rpm += RPM_STEP) {
            double torqueAtRpm  = calcTorque(rpm, maxTorque, hasTurbo);
            double powerAtRpm   = calcPower(rpm, torqueAtRpm);
            torqueCurve.put(rpm, torqueAtRpm);
            powerCurve.put(rpm, powerAtRpm);
        }

        // Escalar para que el pico de potencia coincida con maxPower
        double peakPowerRaw = powerCurve.values().stream()
                .mapToDouble(Double::doubleValue).max().orElse(1.0);
        double scaleFactor = maxPower / peakPowerRaw;

        Map<Integer, Double> scaledPower  = new TreeMap<>();
        Map<Integer, Double> scaledTorque = new TreeMap<>();
        for (int rpm = RPM_MIN; rpm <= RPM_MAX; rpm += RPM_STEP) {
            scaledPower.put(rpm,  powerCurve.get(rpm)  * scaleFactor);
            scaledTorque.put(rpm, torqueCurve.get(rpm) * scaleFactor);
        }

        DynoResult result = new DynoResult();
        result.setMaxPower(maxPower);
        result.setMaxTorque(scaledTorque.values().stream()
                .mapToDouble(Double::doubleValue).max().orElse(maxTorque));
        result.setPowerCurveJson(serializeCurveToJson(scaledPower));
        return result;
    }

    /**
     * Curva de torque realista:
     * - Sube rápido desde ralentí (efecto turbo/llenado de cilindros)
     * - Meseta en el rango medio (donde el motor respira mejor)
     * - Cae suavemente en RPM altas (pérdidas mecánicas y de llenado)
     *
     * Usa una curva logística para la subida + caída exponencial para el drop final.
     */
    private double calcTorque(int rpm, double maxTorque, boolean hasTurbo) {
        double rpmNorm = (double)(rpm - RPM_MIN) / (RPM_MAX - RPM_MIN); // 0..1

        // Punto de pico del torque: ~35-40% del rango RPM (turbo más tarde, NA antes)
        double peakAt   = hasTurbo ? 0.38 : 0.28;
        // Anchura de la meseta
        double plateau  = hasTurbo ? 0.30 : 0.20;

        // Subida: sigmoide
        double riseK    = hasTurbo ? 14.0 : 18.0;  // pendiente de subida
        double rise     = 1.0 / (1.0 + Math.exp(-riseK * (rpmNorm - peakAt * 0.5)));

        // Caída: empieza después de la meseta
        double fallStart = peakAt + plateau;
        double fallK    = hasTurbo ? 5.0 : 6.5;
        double fall;
        if (rpmNorm <= fallStart) {
            fall = 1.0;
        } else {
            fall = Math.exp(-fallK * Math.pow(rpmNorm - fallStart, 1.6));
        }

        // Torque mínimo en ralentí (~55% del pico)
        double base = 0.55;
        double raw  = base + (1.0 - base) * rise * fall;

        return maxTorque * raw;
    }

    /**
     * Potencia = Torque × RPM / 5252  (en unidades anglosajones)
     * En sistema métrico: P(kW) = T(Nm) × ω(rad/s) / 1000
     * Aquí trabajamos en HP internos, escalados al final.
     */
    private double calcPower(int rpm, double torqueAtRpm) {
        // P ∝ T × RPM — escalamos para que el resultado esté en rango razonable
        return torqueAtRpm * rpm / 5252.0;
    }

    // ── Métodos auxiliares para el DynoController ─────────────────────────

    /**
     * Devuelve la curva de potencia completa (RPM → HP) para animación.
     * Usa los mismos cálculos que runDyno para que gráfico y resultado coincidan.
     */
    public Map<Integer, Double> getPowerCurve(Car car, List<Part> parts) {
        double maxPower  = car.getBasePower();
        double maxTorque = car.getBaseTorque();
        if (parts != null) {
            for (Part p : parts) {
                maxPower  += p.getHpDelta();
                maxTorque += p.getTorqueDelta();
            }
        }
        boolean hasTurbo = parts != null && parts.stream()
                .anyMatch(p -> p.getType() != null && p.getType().name().equals("TURBO"));

        Map<Integer, Double> powerCurve = new TreeMap<>();
        for (int rpm = RPM_MIN; rpm <= RPM_MAX; rpm += RPM_STEP) {
            double t = calcTorque(rpm, maxTorque, hasTurbo);
            powerCurve.put(rpm, calcPower(rpm, t));
        }
        double peakRaw = powerCurve.values().stream()
                .mapToDouble(Double::doubleValue).max().orElse(1.0);
        double scale = maxPower / peakRaw;
        powerCurve.replaceAll((k, v) -> v * scale);
        return powerCurve;
    }

    /**
     * Devuelve la curva de torque completa (RPM → Nm) para animación.
     */
    public Map<Integer, Double> getTorqueCurve(Car car, List<Part> parts) {
        double maxTorque = car.getBaseTorque();
        double maxPower  = car.getBasePower();
        if (parts != null) {
            for (Part p : parts) {
                maxTorque += p.getTorqueDelta();
                maxPower  += p.getHpDelta();
            }
        }
        boolean hasTurbo = parts != null && parts.stream()
                .anyMatch(p -> p.getType() != null && p.getType().name().equals("TURBO"));

        Map<Integer, Double> torqueCurve = new TreeMap<>();
        for (int rpm = RPM_MIN; rpm <= RPM_MAX; rpm += RPM_STEP) {
            torqueCurve.put(rpm, calcTorque(rpm, maxTorque, hasTurbo));
        }
        // Escalar igual que en runDyno para consistencia
        Map<Integer, Double> powerCurve = new TreeMap<>();
        for (int rpm = RPM_MIN; rpm <= RPM_MAX; rpm += RPM_STEP) {
            powerCurve.put(rpm, calcPower(rpm, torqueCurve.get(rpm)));
        }
        double peakRaw = powerCurve.values().stream()
                .mapToDouble(Double::doubleValue).max().orElse(1.0);
        double scale = maxPower / peakRaw;
        torqueCurve.replaceAll((k, v) -> v * scale);
        return torqueCurve;
    }

    private String serializeCurveToJson(Map<Integer, Double> curve) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<Integer, Double> e : curve.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(e.getKey()).append("\":")
                    .append(String.format("%.2f", e.getValue()));
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
}