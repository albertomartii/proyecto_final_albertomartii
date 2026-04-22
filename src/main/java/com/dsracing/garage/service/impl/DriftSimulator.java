package com.dsracing.garage.service.impl;

import com.dsracing.garage.model.entity.*;
import com.dsracing.garage.model.entity.*;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class DriftSimulator {

    private final int tickMs = 50;

    public DriftRun simulate(Car car, List<Part> parts, Discipline discipline, long seed) {
        double mass = car.getMass();
        double grip = car.getGripBase();
        double power = car.getBasePower();
        for (Part p : parts) {
            mass += p.getWeightDelta();
            grip += p.getGripDelta();
            power += p.getHpDelta();
        }

        double vLong = 10.0; // m/s
        double vLat = 0.0;
        double score = 0.0;
        double continuity = 0.0;
        List<TelemetrySample> telemetry = new ArrayList<>();
        Random rnd = new Random(seed);

        int steps = 200;
        for (int i = 0; i < steps; i++) {
            double throttle = 0.6 + 0.4 * rnd.nextDouble();
            double steer = (rnd.nextDouble() - 0.5) * 0.4;

            double driveForce = (power / Math.max(vLong, 1.0)) * throttle;
            double lateralAvailable = grip * mass * 9.81;
            vLong += (driveForce / mass) * (tickMs / 1000.0);
            vLat += steer * 0.1;
            double lateralG = vLat / 9.81;

            double angle = Math.atan2(vLat, Math.max(vLong, 0.1));
            double segScore = Math.abs(angle) * 10 + Math.abs(vLat) * 2;
            if (Math.abs(vLat) * mass > lateralAvailable) {
                segScore *= 0.5;
            }

            if (Math.abs(angle) > 0.2) continuity += 1.0;

            TelemetrySample s = new TelemetrySample();
            s.setTimestampMs(i * tickMs);
            s.setRpm(3000 + rnd.nextInt(2000));
            s.setSpeed(vLong);
            s.setLateralG(lateralG);
            s.setYawAngle(Math.toDegrees(angle));
            telemetry.add(s);

            score += segScore;
        }

        DriftRun run = new DriftRun();
        run.setScore(score);
        run.setDurationMs(steps * tickMs);
        run.setMaxAngle(telemetry.stream().mapToDouble(TelemetrySample::getYawAngle).max().orElse(0.0));
        run.setAvgLateralG(telemetry.stream().mapToDouble(TelemetrySample::getLateralG).average().orElse(0.0));
        telemetry.forEach(t -> t.setDriftRun(run));
        run.setTelemetry(telemetry);
        return run;
    }
}
