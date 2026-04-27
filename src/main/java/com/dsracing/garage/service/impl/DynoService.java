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

    // ════════════════════════════════════════════════════════════════════════
    //  API pública
    // ════════════════════════════════════════════════════════════════════════

    public DynoResult runDyno(Car car, List<Part> parts) {
        double maxPower  = totalPower(car, parts);
        double maxTorque = totalTorque(car, parts);
        double massKg    = totalMass(car, parts);
        boolean hasTurbo = hasTurbo(parts);

        // Calcular curvas
        Map<Integer, Double> powerCurve  = new TreeMap<>();
        Map<Integer, Double> torqueCurve = new TreeMap<>();
        for (int rpm = RPM_MIN; rpm <= RPM_MAX; rpm += RPM_STEP) {
            double t = calcTorque(rpm, maxTorque, hasTurbo);
            torqueCurve.put(rpm, t);
            powerCurve.put(rpm, calcPower(rpm, t));
        }

        // Escalar al pico real
        double peakRaw = powerCurve.values().stream()
                .mapToDouble(Double::doubleValue).max().orElse(1.0);
        double scale = maxPower / peakRaw;
        powerCurve.replaceAll((k, v) -> v * scale);
        torqueCurve.replaceAll((k, v) -> v * scale);

        double peakTorque = torqueCurve.values().stream()
                .mapToDouble(Double::doubleValue).max().orElse(maxTorque);

        DynoResult result = new DynoResult();
        result.setMaxPower(maxPower);
        result.setMaxTorque(peakTorque);
        result.setPowerCurveJson(serializeCurveToJson(powerCurve));

        // Tiempos de aceleración
        result.setTime0to60(calc0toN(maxPower, peakTorque, massKg, 60.0 / 3.6));
        result.setTime0to100(calc0toN(maxPower, peakTorque, massKg, 100.0 / 3.6));

        return result;
    }

    /** Curva de potencia (RPM → HP) para la animación del DynoController. */
    public Map<Integer, Double> getPowerCurve(Car car, List<Part> parts) {
        double maxPower  = totalPower(car, parts);
        double maxTorque = totalTorque(car, parts);
        boolean hasTurbo = hasTurbo(parts);

        Map<Integer, Double> powerCurve  = new TreeMap<>();
        Map<Integer, Double> torqueCurve = new TreeMap<>();
        for (int rpm = RPM_MIN; rpm <= RPM_MAX; rpm += RPM_STEP) {
            double t = calcTorque(rpm, maxTorque, hasTurbo);
            torqueCurve.put(rpm, t);
            powerCurve.put(rpm, calcPower(rpm, t));
        }
        double peakRaw = powerCurve.values().stream()
                .mapToDouble(Double::doubleValue).max().orElse(1.0);
        double scale = maxPower / peakRaw;
        powerCurve.replaceAll((k, v) -> v * scale);
        return powerCurve;
    }

    /** Curva de torque (RPM → Nm) para la animación del DynoController. */
    public Map<Integer, Double> getTorqueCurve(Car car, List<Part> parts) {
        double maxPower  = totalPower(car, parts);
        double maxTorque = totalTorque(car, parts);
        boolean hasTurbo = hasTurbo(parts);

        Map<Integer, Double> powerCurve  = new TreeMap<>();
        Map<Integer, Double> torqueCurve = new TreeMap<>();
        for (int rpm = RPM_MIN; rpm <= RPM_MAX; rpm += RPM_STEP) {
            double t = calcTorque(rpm, maxTorque, hasTurbo);
            torqueCurve.put(rpm, t);
            powerCurve.put(rpm, calcPower(rpm, t));
        }
        double peakRaw = powerCurve.values().stream()
                .mapToDouble(Double::doubleValue).max().orElse(1.0);
        double scale = maxPower / peakRaw;
        torqueCurve.replaceAll((k, v) -> v * scale);
        return torqueCurve;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Física de aceleración — 0 a N km/h
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Integración numérica (dt=5ms) del movimiento del coche desde parado.
     * Modela: fuerza motriz, límite de adherencia, resistencia aerodinámica,
     * resistencia de rodadura y pérdida momentánea por cambio de marcha.
     *
     * @param maxPowerHp  Potencia máxima en CV
     * @param maxTorqueNm Torque máximo en Nm
     * @param massKg      Masa total del coche en kg
     * @param targetMs    Velocidad objetivo en m/s
     * @return            Tiempo en segundos, o -1 si no se alcanza
     */
    private double calc0toN(double maxPowerHp, double maxTorqueNm,
                            double massKg, double targetMs) {
        double maxPowerW     = maxPowerHp * 745.7;
        double wheelRadius   = 0.30;
        double gearRatio     = 4.0;
        double drivetrainEff = 0.88;
        double cd            = 0.32;
        double frontalArea   = 2.0;
        double airDensity    = 1.225;
        double crr           = 0.013;
        double g             = 9.81;
        double gripLimit     = gripEstimate(massKg) * massKg * g;

        double v   = 0.0;
        double t   = 0.0;
        double dt  = 0.005;
        double max = 60.0;

        while (v < targetMs && t < max) {
            double fEngine;
            if (v < 0.5) {
                fEngine = (maxTorqueNm * gearRatio * drivetrainEff) / wheelRadius;
            } else {
                fEngine = (maxPowerW * drivetrainEff) / v;
                fEngine *= gearShiftFactor(v);
            }
            fEngine = Math.min(fEngine, gripLimit);

            double fAero  = 0.5 * airDensity * cd * frontalArea * v * v;
            double fRoll  = crr * massKg * g;
            double fTotal = fEngine - fAero - fRoll;

            v += Math.max(0, fTotal / massKg) * dt;
            t += dt;
        }
        return t < max ? t : -1;
    }

    /** Pérdida de tracción al cambiar de marcha (0–1). */
    private double gearShiftFactor(double v) {
        double[] shifts = {13.9, 25.0, 38.9, 55.6}; // ~50, 90, 140, 200 km/h
        for (double sp : shifts) {
            if (Math.abs(v - sp) < 1.5) return 0.94;
        }
        return 1.0;
    }

    /** Coeficiente de adherencia estimado por masa (más ligero = más grip relativo). */
    private double gripEstimate(double massKg) {
        return Math.max(0.9, 1.4 - massKg / 4000.0);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Curvas de motor
    // ════════════════════════════════════════════════════════════════════════

    private double calcTorque(int rpm, double maxTorque, boolean hasTurbo) {
        double norm     = (double)(rpm - RPM_MIN) / (RPM_MAX - RPM_MIN);
        double peakAt   = hasTurbo ? 0.38 : 0.28;
        double plateau  = hasTurbo ? 0.30 : 0.20;
        double riseK    = hasTurbo ? 14.0 : 18.0;
        double rise     = 1.0 / (1.0 + Math.exp(-riseK * (norm - peakAt * 0.5)));
        double fallStart = peakAt + plateau;
        double fallK    = hasTurbo ? 5.0 : 6.5;
        double fall     = norm <= fallStart ? 1.0
                : Math.exp(-fallK * Math.pow(norm - fallStart, 1.6));
        return maxTorque * (0.55 + 0.45 * rise * fall);
    }

    private double calcPower(int rpm, double torque) {
        return torque * rpm / 5252.0;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Helpers
    // ════════════════════════════════════════════════════════════════════════

    private double totalPower(Car car, List<Part> parts) {
        double v = car.getBasePower();
        if (parts != null) for (Part p : parts) v += p.getHpDelta();
        return v;
    }

    private double totalTorque(Car car, List<Part> parts) {
        double v = car.getBaseTorque();
        if (parts != null) for (Part p : parts) v += p.getTorqueDelta();
        return v;
    }

    private double totalMass(Car car, List<Part> parts) {
        double v = car.getMass();
        if (parts != null) for (Part p : parts) v += p.getWeightDelta();
        return v;
    }

    private boolean hasTurbo(List<Part> parts) {
        return parts != null && parts.stream()
                .anyMatch(p -> p.getType() != null && p.getType().name().equals("TURBO"));
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
        return sb.append("}").toString();
    }
}