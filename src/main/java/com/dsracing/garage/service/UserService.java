package com.dsracing.garage.service;

import com.dsracing.garage.model.entity.User;
import java.util.Optional;

public interface UserService {
    Optional<User> login(String username, String password);
    User register(String username, String password, String email);
    boolean existsByUsername(String username);
}