package com.reel2real.backend.repository;

import com.reel2real.backend.entity.Trip;
import com.reel2real.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.List;

public interface TripRepository extends JpaRepository<Trip, UUID> {

    List<Trip> findByUser(User user);

}
