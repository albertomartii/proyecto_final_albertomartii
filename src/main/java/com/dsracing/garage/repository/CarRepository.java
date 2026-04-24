package com.dsracing.garage.repository;

import com.dsracing.garage.model.entity.Car;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CarRepository extends JpaRepository<Car, Long> {

    @Query("SELECT c FROM Car c WHERE c.garage.owner.id = :userId")
    List<Car> findByGarageOwnerId(@Param("userId") Long userId);
}
    /*@Query("SELECT c FROM Car c JOIN c.parts p WHERE c.basePower > :hp AND p.type = com.dsracing.garage.model.entity.PartType.TURBO")
    List<Car> findCarsWithTurboAndPowerGreaterThan(double hp);
}*/
