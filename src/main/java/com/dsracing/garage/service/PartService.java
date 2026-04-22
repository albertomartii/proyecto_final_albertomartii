package com.dsracing.garage.service;

import com.dsracing.garage.model.entity.Part;
import com.dsracing.garage.model.entity.PartType;

import java.util.List;
import java.util.Optional;

public interface PartService {
    List<Part> findAll();
    Optional<Part> findById(Long id);
    List<Part> findByType(PartType type);
    Part save(Part part);
    void deleteById(Long id);
}
