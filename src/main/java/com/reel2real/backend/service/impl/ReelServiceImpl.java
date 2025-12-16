package com.reel2real.backend.service.impl;

import com.reel2real.backend.dto.reel.ReelCreateRequest;
import com.reel2real.backend.dto.reel.ReelResponse;
import com.reel2real.backend.entity.Place;
import com.reel2real.backend.entity.Reel;
import com.reel2real.backend.entity.User;
import com.reel2real.backend.exception.ResourceNotFoundException;
import com.reel2real.backend.repository.PlaceRepository;
import com.reel2real.backend.repository.ReelRepository;
import com.reel2real.backend.repository.UserRepository;
import com.reel2real.backend.service.ReelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReelServiceImpl implements ReelService {

    private final ReelRepository reelRepository;
    private final UserRepository userRepository;
    private final PlaceRepository placeRepository;

    @Override
    public ReelResponse addReel(UUID userId, ReelCreateRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Place place = null;
        if (request.getPlaceName() != null && request.getCity() != null) {
            place = Place.builder()
                    .name(request.getPlaceName())
                    .city(request.getCity())
                    .build();
            place = placeRepository.save(place);
        }

        Reel reel = Reel.builder()
                .user(user)
                .reelUrl(request.getReelUrl())
                .platform(request.getPlatform())
                .notes(request.getNotes())
                .place(place)
                .createdAt(LocalDateTime.now())
                .build();

        Reel saved = reelRepository.save(reel);

        return ReelResponse.builder()
                .id(saved.getId())
                .reelUrl(saved.getReelUrl())
                .platform(saved.getPlatform())
                .notes(saved.getNotes())
                .placeName(place != null ? place.getName() : null)
                .build();
    }

    @Override
    public List<ReelResponse> getReelsForUser(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return reelRepository.findByUser(user)
                .stream()
                .map(reel -> ReelResponse.builder()
                        .id(reel.getId())
                        .reelUrl(reel.getReelUrl())
                        .platform(reel.getPlatform())
                        .notes(reel.getNotes())
                        .placeName(
                                reel.getPlace() != null ? reel.getPlace().getName() : null
                        )
                        .build())
                .collect(Collectors.toList());
    }
}
