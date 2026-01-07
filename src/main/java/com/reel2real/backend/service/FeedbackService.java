package com.reel2real.backend.service;
import java.util.List;
import java.util.UUID;

import com.reel2real.backend.dto.feedback.FeedbackRequest;

public interface FeedbackService {
    void submitFeedback(FeedbackRequest request);

}
