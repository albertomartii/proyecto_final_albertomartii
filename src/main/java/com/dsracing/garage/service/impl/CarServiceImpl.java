package com.dsracing.garage.service.impl;

import com.dsracing.garage.model.entity.Car;
import com.dsracing.garage.repository.CarRepository;
import com.dsracing.garage.service.CarService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CarServiceImpl implements CarService {

    private final CarRepository carRepository;

    public CarServiceImpl(CarRepository carRepository) {
        this.carRepository = carRepository;
    }

    @Override
    public List<Car> findAll() {
        return carRepository.findAll();
    }

    @Override
    public Optional<Car> findById(Long id) {
        return carRepository.findById(id);
    }

    @Override
    public Car save(Car car) {
        return carRepository.save(car);
    }

    @Override
    public void deleteById(Long id) {
        carRepository.deleteById(id);
    }

    // ── Nuevos métodos con sesión abierta ──────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Optional<Car> findByIdWithParts(Long id) {
        Optional<Car> car = carRepository.findById(id);
        car.ifPresent(c -> c.getParts().size()); // inicializa la colección lazy
        return car;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Car> findAllWithParts() {
        List<Car> cars = carRepository.findAll();
        cars.forEach(c -> c.getParts().size()); // inicializa cada colección lazy
        return cars;
    }
    @Override
    @Transactional(readOnly = true)
    public List<Car> findByUserId(Long userId) {
        List<Car> cars = carRepository.findByGarageOwnerId(userId);
        cars.forEach(c -> c.getParts().size()); // inicializa lazy
        return cars;
    }
}