package com.dsracing.garage.repository;

import com.dsracing.garage.model.entity.Garage;
import com.dsracing.garage.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GarageRepository extends JpaRepository<Garage, Long> {
    List<Garage> findByOwner(User owner);
}
