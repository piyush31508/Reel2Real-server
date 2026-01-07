package com.reel2real.backend.controller;

import com.reel2real.backend.dto.feedback.FeedbackRequest;
import com.reel2real.backend.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    public ResponseEntity<Void> submit(
            @Valid @RequestBody FeedbackRequest request
    ) {
        feedbackService.submitFeedback(request);
        return ResponseEntity.ok().build();
    }
}
