package com.reel2real.backend.service.impl;

import com.reel2real.backend.dto.user.UserCreateRequest;
import com.reel2real.backend.dto.user.UserResponse;
import com.reel2real.backend.exception.BadRequestException;
import com.reel2real.backend.exception.ResourceNotFoundException;
import com.reel2real.backend.service.UserService;
import com.reel2real.backend.entity.User;
import com.reel2real.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;


    @Override
    public UserResponse createUser(UserCreateRequest request) {
        if(userRepository.existsByEmail(request.getEmail())){
            throw new BadRequestException("Email already registered");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(request.getPassword())
                .createdAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);

        return UserResponse.builder()
                .id(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .build();
    }

    @Override
    public UserResponse getUserById(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }
}
