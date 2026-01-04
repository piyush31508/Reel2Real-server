package com.reel2real.backend.service;

import com.reel2real.backend.dto.budget.BudgetRealityResponse;
import com.reel2real.backend.entity.Place;
import com.reel2real.backend.entity.Trip;

import java.util.List;

public interface BudgetRealityService {

    BudgetRealityResponse calculate(List<Place> places, Trip trip);
}
