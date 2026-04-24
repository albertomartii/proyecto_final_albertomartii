package com.dsracing.garage.service;

import com.dsracing.garage.model.entity.Car;
import java.util.List;
import java.util.Optional;

public interface CarService {
    List<Car> findAll();
    Optional<Car> findById(Long id);
    Car save(Car car);
    void deleteById(Long id);
    Optional<Car> findByIdWithParts(Long id);
    List<Car> findAllWithParts();
    List<Car> findByUserId(Long userId); // coches del usuario logueado
}