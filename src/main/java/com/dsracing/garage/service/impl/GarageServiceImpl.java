package com.dsracing.garage.service.impl;

import com.dsracing.garage.model.entity.Garage;
import com.dsracing.garage.model.entity.User;
import com.dsracing.garage.repository.GarageRepository;
import com.dsracing.garage.service.GarageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GarageServiceImpl implements GarageService {

    private final GarageRepository garageRepository;

    public GarageServiceImpl(GarageRepository garageRepository) {
        this.garageRepository = garageRepository;
    }

    @Override
    @Transactional
    public Garage getOrCreateGarageForUser(User user) {
        List<Garage> garages = garageRepository.findByOwnerId(user.getId());
        if (!garages.isEmpty()) {
            return garages.get(0);
        }
        Garage garage = new Garage();
        garage.setName(user.getUsername() + "'s Garage");
        garage.setLocation("Unknown");
        garage.setOwner(user);
        return garageRepository.save(garage);
    }
}