package com.dsracing.garage.repository;

import com.dsracing.garage.model.entity.Part;
import com.dsracing.garage.model.entity.PartType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PartRepository extends JpaRepository<Part, Long> {

    List<Part> findByType(PartType type);

    /**
     * Busca una pieza por nombre exacto.
     * Usado por PartEditorController para resolver piezas del catálogo a entidades con ID.
     */
    Optional<Part> findByName(String name);
}