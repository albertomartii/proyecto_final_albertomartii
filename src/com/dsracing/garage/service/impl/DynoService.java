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

    public DynoResult runDyno(Car car, List<Part> parts) {
        double power = car.getBasePower();
        double torque = car.getBaseTorque();
        double mass = car.getMass();

        for (Part p : parts) {
            power += p.getHpDelta();
            torque += p.getTorqueDelta();
            mass += p.getWeightDelta();
        }

        Map<Integer, Double> curve = new TreeMap<>();
        for (int rpm = 1000; rpm <= 8000; rpm += 250) {
            double factor = Math.exp(-Math.pow((rpm - 5000) / 2000.0, 2));
            double pAtRpm = power * (0.6 + 0.4 * factor);
            curve.put(rpm, pAtRpm);
        }

        DynoResult result = new DynoResult();
        result.setMaxPower(curve.values().stream().mapToDouble(Double::doubleValue).max().orElse(power));
        result.setMaxTorque(torque);
        result.setPowerCurveJson(serializeCurveToJson(curve));
        return result;
    }

    private String serializeCurveToJson(Map<Integer, Double> curve) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Map.Entry<Integer, Double> e : curve.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(e.getKey()).append("\":").append(String.format("%.2f", e.getValue()));
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
}
