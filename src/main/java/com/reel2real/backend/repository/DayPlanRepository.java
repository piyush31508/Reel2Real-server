package com.reel2real.backend.repository;

import com.reel2real.backend.entity.DayPlan;
import com.reel2real.backend.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DayPlanRepository extends JpaRepository<DayPlan, UUID> {

    List<DayPlan> findByTripOrderByDayNumber(Trip trip);
}
