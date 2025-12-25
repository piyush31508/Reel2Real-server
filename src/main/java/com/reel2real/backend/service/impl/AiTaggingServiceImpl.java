package com.reel2real.backend.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reel2real.backend.dto.reel.AiTagResponse;
import com.reel2real.backend.integration.OpenRouterClient;
import com.reel2real.backend.service.AiTaggingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiTaggingServiceImpl implements AiTaggingService {

    private final ObjectMapper mapper = new ObjectMapper();
    private final OpenRouterClient openRouterClient;

    @Override
    public AiTagResponse analyze(String caption, String hashtags) {

        String prompt = buildPrompt(caption, hashtags);

        // 🔥 Call OpenRouter API
        String rawResponse = callOpenRouter(prompt);

        System.out.println("🔹 RAW LLM OUTPUT:");
        System.out.println(rawResponse);

        AiTagResponse parsed = parseJson(rawResponse);

        System.out.println("🔹 PARSED OUTPUT:");
        System.out.println(parsed);

        return parsed;
    }

    private String buildPrompt(String caption, String hashtags) {
        return
                "Extract travel location info from text. " +
                        "Return only a valid JSON object with no commentary.\n" +
                        "Fields: placeName, city, country, category.\n\n" +
                        "**Caption:** " + caption + "\n" +
                        "**Hashtags:** " + hashtags + "\n\n" +
                        "Output format strictly:\n" +
                        "{ \"placeName\": \"\", \"city\": \"\", \"country\": \"\", \"category\": \"\" }";
    }

    private String callOpenRouter(String prompt) {
        return openRouterClient.generate(prompt);
    }

    private AiTagResponse parseJson(String raw) {
        try {
            String cleaned = raw
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();

            return mapper.readValue(cleaned, AiTagResponse.class);

        } catch (Exception e) {
            e.printStackTrace(); // add this
            return AiTagResponse.builder().build();
        }
    }

}
