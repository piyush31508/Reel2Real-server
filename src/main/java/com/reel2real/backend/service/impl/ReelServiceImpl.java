package com.reel2real.backend.service.impl;

import com.reel2real.backend.dto.reel.AiTagResponse;
import com.reel2real.backend.dto.reel.ReelCreateRequest;
import com.reel2real.backend.dto.reel.ReelResponse;
import com.reel2real.backend.entity.Place;
import com.reel2real.backend.entity.Reel;
import com.reel2real.backend.entity.User;
import com.reel2real.backend.exception.ResourceNotFoundException;
import com.reel2real.backend.repository.PlaceRepository;
import com.reel2real.backend.repository.ReelRepository;
import com.reel2real.backend.repository.UserRepository;
import com.reel2real.backend.service.AiTaggingService;
import com.reel2real.backend.service.ReelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final AiTaggingService aiTaggingService;

    @Override
    @Transactional
    public ReelResponse addReel(UUID userId, ReelCreateRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 🔹 AI TAG
        AiTagResponse aiTag = null;
        if (hasText(request.getCaption()) || hasText(request.getHashtags())) {
            aiTag = aiTaggingService.analyze(
                    request.getCaption(),
                    request.getHashtags()
            );
        }

        // 🔹 Merge manual + AI
        String placeName = pick(request.getPlaceName(), aiTag != null ? aiTag.getPlaceName() : null);
        String city = pick(request.getCity(), aiTag != null ? aiTag.getCity() : null);
        String country = pick(request.getCountry(), aiTag != null ? aiTag.getCountry() : null);
        String category = aiTag != null ? aiTag.getCategory() : null;

        Place place = null;

        if (hasText(placeName) && hasText(city)) {

            List<Place> matches =
                    placeRepository.findByNameIgnoreCaseAndCityIgnoreCase(placeName, city);

            if (!matches.isEmpty()) {
                // Pick first
                place = matches.get(0);

                // Update missing fields
                boolean updated = false;

                if (country != null && (place.getCountry() == null || place.getCountry().isBlank())) {
                    place.setCountry(country);
                    updated = true;
                }
                if (category != null && (place.getCategory() == null || place.getCategory().isBlank())) {
                    place.setCategory(category);
                    updated = true;
                }

                if (updated) {
                    place = placeRepository.save(place);
                }

            } else {
                // Create new place
                place = placeRepository.save(
                        Place.builder()
                                .name(placeName)
                                .city(city)
                                .country(country)
                                .category(category)
                                .build()
                );
            }
        }

        // 🔹 Create Reel
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
                .country(place != null ? place.getCountry() : null)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
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
                        .country(
                                reel.getPlace() != null ? reel.getPlace().getCountry() : null
                        )
                        .build()
                ).collect(Collectors.toList());
    }

    private boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private String pick(String manual, String ai) {
        return hasText(manual) ? manual : ai;
    }
}
