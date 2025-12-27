package com.reel2real.backend.controller;

import com.reel2real.backend.dto.reel.AiTagResponse;
import com.reel2real.backend.service.AiTaggingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai-tag")
@RequiredArgsConstructor
public class AiTaggingController {

    private final AiTaggingService aiTaggingService;

    @PostMapping
    public ResponseEntity<AiTagResponse> analyze(
            @RequestParam(required = false) String caption,
            @RequestParam(required = false) String hashtags
    ) {
        return ResponseEntity.ok(
                aiTaggingService.analyze(caption, hashtags)
        );
    }
}
