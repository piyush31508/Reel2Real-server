package com.reel2real.backend.repository;

import com.reel2real.backend.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PlaceRepository extends JpaRepository<Place, UUID> {

    List<Place> findByNameIgnoreCase(String name);

    List<Place> findByNameIgnoreCaseAndCityIgnoreCase(String name, String city);

    List<Place> findByCityIgnoreCase(String city);

    List<Place> findByCityIgnoreCaseAndCategoryIgnoreCase(String city, String category);
}
