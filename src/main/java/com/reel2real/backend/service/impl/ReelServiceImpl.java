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
@Transactional
public class ReelServiceImpl implements ReelService {

    private final ReelRepository reelRepository;
    private final UserRepository userRepository;
    private final PlaceRepository placeRepository;
    private final AiTaggingService aiTaggingService;

    @Override
    public ReelResponse addReel(UUID userId, ReelCreateRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        AiTagResponse ai = hasAIInput(request)
                ? aiTaggingService.analyze(request.getCaption(), request.getHashtags())
                : null;

        String placeName = pick(request.getPlaceName(), ai != null ? ai.getPlaceName() : null);
        String city = pick(request.getCity(), ai != null ? ai.getCity() : null);
        String country = pick(request.getCountry(), ai != null ? ai.getCountry() : null);

        Place place = resolvePlace(placeName, city, country, ai);

        Reel reel = reelRepository.save(
                Reel.builder()
                        .user(user)
                        .reelUrl(request.getReelUrl())
                        .platform(request.getPlatform())
                        .notes(request.getNotes())
                        .place(place)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        return mapToResponse(reel);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReelResponse> getReelsForUser(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return reelRepository.findByUser(user)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private Place resolvePlace(
            String name, String city, String country, AiTagResponse ai
    ) {
        if (!hasText(name) || !hasText(city)) return null;

        return placeRepository
                .findFirstByNameIgnoreCaseAndCityIgnoreCase(name, city)
                .map(existing -> updateIfMissing(existing, country, ai))
                .orElseGet(() ->
                        placeRepository.save(
                                Place.builder()
                                        .name(name)
                                        .city(city)
                                        .country(country)
                                        .category(ai != null ? ai.getCategory() : null)
                                        .activity(ai != null ? ai.getActivity() : null)
                                        .crowdLevel(ai != null ? ai.getCrowdLevel() : null)
                                        .budgetLevel(ai != null ? ai.getBudgetLevel() : null)
                                        .safetyNote(ai != null ? ai.getSafetyNote() : null)
                                        .build()
                        )
                );
    }

    private Place updateIfMissing(Place p, String country, AiTagResponse ai) {
        boolean updated = false;

        if (p.getCountry() == null && country != null) {
            p.setCountry(country);
            updated = true;
        }
        if (ai != null && p.getCategory() == null) {
            p.setCategory(ai.getCategory());
            updated = true;
        }

        return updated ? placeRepository.save(p) : p;
    }

    private ReelResponse mapToResponse(Reel r) {
        return ReelResponse.builder()
                .id(r.getId())
                .reelUrl(r.getReelUrl())
                .platform(r.getPlatform())
                .notes(r.getNotes())
                .placeName(r.getPlace() != null ? r.getPlace().getName() : null)
                .country(r.getPlace() != null ? r.getPlace().getCountry() : null)
                .build();
    }

    private boolean hasAIInput(ReelCreateRequest r) {
        return hasText(r.getCaption()) || hasText(r.getHashtags());
    }

    private boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private String pick(String manual, String ai) {
        return hasText(manual) ? manual : ai;
    }
}
