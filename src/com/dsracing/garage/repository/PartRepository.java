package com.dsracing.garage.repository;

import com.dsracing.garage.model.entity.Part;
import com.dsracing.garage.model.entity.PartType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PartRepository extends JpaRepository<Part, Long> {
    List<Part> findByType(PartType type);
}
