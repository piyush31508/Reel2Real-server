package com.reel2real.backend.service;

import com.reel2real.backend.dto.reel.ReelCreateRequest;
import com.reel2real.backend.dto.reel.ReelResponse;

import java.util.List;
import java.util.UUID;

public interface ReelService {

    ReelResponse addReel(UUID userId, ReelCreateRequest request);

    List<ReelResponse> getReelsForUser(UUID userId);
}
