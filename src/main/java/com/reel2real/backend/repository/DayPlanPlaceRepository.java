package com.reel2real.backend.repository;

import com.reel2real.backend.entity.DayPlan;
import com.reel2real.backend.entity.DayPlanPlace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DayPlanPlaceRepository extends JpaRepository<DayPlanPlace, UUID> {

    List<DayPlanPlace> findByDayPlanOrderByVisitOrder(DayPlan dayPlan);
}
