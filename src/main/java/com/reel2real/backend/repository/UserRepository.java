package com.reel2real.backend.repository;

import com.reel2real.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface  UserRepository extends JpaRepository<User, UUID>{

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

}
