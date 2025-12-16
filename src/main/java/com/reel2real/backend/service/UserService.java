package com.reel2real.backend.service;

import com.reel2real.backend.dto.user.UserCreateRequest;
import com.reel2real.backend.dto.user.UserResponse;

import java.util.UUID;

public interface UserService {

    UserResponse createUser(UserCreateRequest request);

    UserResponse getUserById(UUID userId);

}
