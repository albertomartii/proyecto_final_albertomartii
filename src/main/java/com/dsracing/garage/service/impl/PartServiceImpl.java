package com.dsracing.garage.service.impl;

import com.dsracing.garage.model.entity.Part;
import com.dsracing.garage.model.entity.PartType;
import com.dsracing.garage.repository.PartRepository;
import com.dsracing.garage.service.PartService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PartServiceImpl implements PartService {

    private final PartRepository partRepository;

    public PartServiceImpl(PartRepository partRepository) {
        this.partRepository = partRepository;
    }

    @Override
    public List<Part> findAll() {
        return partRepository.findAll();
    }

    @Override
    public Optional<Part> findById(Long id) {
        return partRepository.findById(id);
    }

    @Override
    public List<Part> findByType(PartType type) {
        return partRepository.findByType(type);
    }

    @Override
    public Part save(Part part) {
        return partRepository.save(part);
    }

    @Override
    public void deleteById(Long id) {
        partRepository.deleteById(id);
    }
}
