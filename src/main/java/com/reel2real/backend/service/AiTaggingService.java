package com.reel2real.backend.service;

import com.reel2real.backend.dto.reel.AiTagResponse;

public interface AiTaggingService {
    AiTagResponse analyze(String caption, String hashtags);
}
