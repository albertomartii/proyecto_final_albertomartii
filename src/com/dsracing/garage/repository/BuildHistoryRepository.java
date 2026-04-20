package com.dsracing.garage.repository;

import com.dsracing.garage.model.entity.BuildHistory;
import com.dsracing.garage.model.entity.Discipline;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BuildHistoryRepository extends JpaRepository<BuildHistory, Long> {
    List<BuildHistory> findByTargetDisciplineOrderByDynoResultMaxPowerDesc(Discipline discipline, Pageable pageable);
}
