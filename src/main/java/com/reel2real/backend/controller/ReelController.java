package com.reel2real.backend.controller;

import com.reel2real.backend.dto.reel.ReelCreateRequest;
import com.reel2real.backend.dto.reel.ReelResponse;
import com.reel2real.backend.service.ReelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reels")
@RequiredArgsConstructor
public class ReelController {

    private final ReelService reelService;

    @PostMapping("/user/{userId}")
    public ResponseEntity<ReelResponse> addReel(
            @PathVariable UUID userId,
            @Valid @RequestBody ReelCreateRequest request
    ) {
        return new ResponseEntity<>(
                reelService.addReel(userId, request),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ReelResponse>> getReels(
            @PathVariable UUID userId
    ) {
        return ResponseEntity.ok(
                reelService.getReelsForUser(userId)
        );
    }
}
