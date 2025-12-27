package com.reel2real.backend.repository;

import com.reel2real.backend.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlaceRepository extends JpaRepository<Place, UUID> {

    Optional<Place> findFirstByNameIgnoreCaseAndCityIgnoreCase(
            String name, String city
    );

    List<Place> findByCityIgnoreCase(String city);

    List<Place> findByCityIgnoreCaseAndCategoryIgnoreCase(
            String city, String category
    );
}
