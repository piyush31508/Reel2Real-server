package com.reel2real.backend.repository;

import com.reel2real.backend.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PlaceRepository extends JpaRepository<Place, UUID> {

    List<Place> findByCity(String city);

    List<Place> findByCityAndCategory(String city, String category);
}
