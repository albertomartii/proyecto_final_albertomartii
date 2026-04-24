package com.dsracing.garage.service;

import com.dsracing.garage.model.entity.Garage;
import com.dsracing.garage.model.entity.User;

public interface GarageService {
    Garage getOrCreateGarageForUser(User user);
}