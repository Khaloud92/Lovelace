package com.sep.sep_backend.SighseeingSugesstion;

import com.sep.sep_backend.budget.Budget;
import com.sep.sep_backend.budget.BudgetRepository;
import com.sep.sep_backend.cost.Cost;
import com.sep.sep_backend.SighseeingSugesstion.dto.AiSightseeingListResponse;
import com.sep.sep_backend.travelentry.TravelEntry;
import com.sep.sep_backend.travelentry.TravelEntryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service responsible for fetching AI-driven sightseeing suggestions based on travel destinations.
 */
@Service
public class SightseeingService {

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Value("${groq.api.url}")
    private String groqUrl;

    private final RestTemplate restTemplate;
    private final TravelEntryRepository travelEntryRepository;
    private final BudgetRepository budgetRepository;
    private final ObjectMapper objectMapper;

    public SightseeingService(RestTemplate restTemplate, TravelEntryRepository travelEntryRepository, BudgetRepository budgetRepository) {
        this.restTemplate = restTemplate;
        this.travelEntryRepository = travelEntryRepository;
        this.budgetRepository = budgetRepository;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Generates the top three sightseeing recommendations with estimated costs for a given budget ID.
     * Maps the AI response from the external LLM provider into temporary Cost entities.
     *
     * @param budgetId The UUID of the budget associated with the travel entry.
     * @return A list of temporary Cost objects representing the suggested sights.
     * @throws Exception If database entities are missing or the external API call fails.
     */
    @SuppressWarnings("unchecked")
    public List<Cost> getTopThreeSightsForTravelEntry(UUID budgetId) throws Exception {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new IllegalArgumentException("Budget mit ID " + budgetId + " nicht gefunden"));

        TravelEntry travelEntry = budget.getTravelEntry();
        if (travelEntry == null) {
            throw new IllegalStateException("Dieses Budget ist mit keinem Reiseeintrag verknüpft.");
        }

        String location = travelEntry.getDestination();

        String prompt = "Du bist ein Reiseassistent. Nenne mir die top 3 Sehenswürdigkeiten in '" + location + "'. " +
                "Gib mir für jede Sehenswürdigkeit einen geschätzten Eintrittspreis bzw. Kostenfaktor in Euro an. " +
                "Wenn es kostenlos ist, trage 0.0 ein. " +
                "Antworte AUSSCHLIESSLICH mit einem validen JSON-Objekt. " +
                "Das Format MUSS exakt so aussehen: {\"sights\": [{\"name\": \"Name der Sehenswürdigkeit\", \"amount\": 12.50}]}. " +
                "Kein Markdown, kein Text davor oder danach!";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + groqApiKey);

        Map<String, Object> message = Map.of("role", "user", "content", prompt);
        Map<String, Object> requestBody = Map.of(
                "model", "llama-3.3-70b-versatile",
                "messages", List.of(message),
                "temperature", 0.0
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        Map<String, Object> response = restTemplate.postForObject(groqUrl, request, Map.class);

        // The model returns its JSON answer as a string nested inside the chat completion response.
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> firstChoice = choices.get(0);
        Map<String, Object> responseMessage = (Map<String, Object>) firstChoice.get("message");
        String jsonContent = (String) responseMessage.get("content");

        AiSightseeingListResponse aiResponse = objectMapper.readValue(jsonContent, AiSightseeingListResponse.class);

        return aiResponse.sights().stream().map(dto -> {
            Cost cost = new Cost();
            cost.setName(dto.name());
            cost.setAmount(dto.amount());
            cost.setBudget(budget);
            return cost;
        }).collect(Collectors.toList());
    }
}
