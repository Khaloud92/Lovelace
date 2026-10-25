package com.sep.sep_backend.cost;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep.sep_backend.Filemanager.DocumentService;
import com.sep.sep_backend.budget.Budget;
import com.sep.sep_backend.budget.BudgetRepository;
import com.sep.sep_backend.cost.dto.response.AiCostListResponse;
import com.sep.sep_backend.travelentry.TravelEntry;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Extracts the total amount from uploaded receipts (PDF or image) via the Groq API
 * and stores the result as a cost of the given budget.
 */
@Service
public class CostDocumentScanService {

    private static final String TEXT_MODEL = "llama-3.3-70b-versatile";
    private static final String VISION_MODEL = "meta-llama/llama-4-scout-17b-16e-instruct";

    private static final String PROMPT =
            "Du bist ein Reiseassistent. Extrahiere aus diesem Beleg NUR den zu zahlenden Gesamtbetrag (TOTAL). " +
            "KEINE einzelnen Posten, KEINE Zwischensumme, KEINE Mehrwertsteuer, KEIN Trinkgeld. " +
            "Als Name verwende den Namen des Händlers/Restaurants/Anbieters. " +
            "Antworte AUSSCHLIESSLICH mit einem validen JSON-Objekt im Format: {\"costs\": [{\"name\": \"Händlername\", \"amount\": 12.50}]} " +
            "mit genau EINEM Eintrag. Betrag als Zahl in Euro. " +
            "Wenn kein Betrag gefunden wird, antworte mit {\"costs\": []}. " +
            "Kein Markdown, kein Text davor oder danach!";

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Value("${groq.api.url}")
    private String groqUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final BudgetRepository budgetRepository;
    private final CostRepository costRepository;
    private final DocumentService documentService;

    public CostDocumentScanService(RestTemplate restTemplate,
                                   BudgetRepository budgetRepository,
                                   CostRepository costRepository,
                                   DocumentService documentService) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
        this.budgetRepository = budgetRepository;
        this.costRepository = costRepository;
        this.documentService = documentService;
    }

    /**
     * Scans a receipt, adds the extracted total as a cost to the budget, and archives the file.
     *
     * @param budgetId id of the budget the cost belongs to
     * @param file     the receipt (PDF, PNG, or JPEG)
     * @return the saved costs
     * @throws NoCostsFoundException if no usable amount is found in the document
     */
    @Transactional
    public List<Cost> scanDocumentAndAddCosts(UUID budgetId, MultipartFile file) throws Exception {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new IllegalArgumentException("Budget nicht gefunden mit ID: " + budgetId));

        TravelEntry travelEntry = budget.getTravelEntry();
        if (travelEntry == null) {
            throw new IllegalStateException("Dieses Budget ist mit keinem Reiseeintrag verknüpft.");
        }

        String contentType = file.getContentType() != null ? file.getContentType() : "";
        String aiContent;
        if (contentType.equals("application/pdf")) {
            aiContent = scanPdf(file);
        } else if (contentType.equals("image/png") || contentType.equals("image/jpeg")) {
            aiContent = scanImage(file, contentType);
        } else {
            throw new IllegalArgumentException("Nur PNG, JPEG oder PDF Dateien werden unterstützt.");
        }

        List<Cost> costs = parseCosts(aiContent, budget);
        if (costs.isEmpty()) {
            throw new NoCostsFoundException("Couldn't find any costs");
        }

        documentService.saveDocument(travelEntry.getId(), file);
        return costRepository.saveAll(costs);
    }

    private String scanPdf(MultipartFile file) throws Exception {
        String text;
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            text = new PDFTextStripper().getText(document);
        }
        if (text.isBlank()) {
            throw new NoCostsFoundException("Couldn't find any costs");
        }
        Map<String, Object> message = Map.of("role", "user", "content", PROMPT + "\n\nDokument:\n" + text);
        return callGroq(TEXT_MODEL, message);
    }

    private String scanImage(MultipartFile file, String contentType) throws Exception {
        String dataUrl = "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(file.getBytes());
        Map<String, Object> message = Map.of("role", "user", "content", List.of(
                Map.of("type", "text", "text", PROMPT),
                Map.of("type", "image_url", "image_url", Map.of("url", dataUrl))
        ));
        return callGroq(VISION_MODEL, message);
    }

    @SuppressWarnings("unchecked")
    private String callGroq(String model, Map<String, Object> message) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + groqApiKey);

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(message),
                "temperature", 0.0
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        Map<String, Object> response = restTemplate.postForObject(groqUrl, request, Map.class);

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> responseMessage = (Map<String, Object>) choices.get(0).get("message");
        return (String) responseMessage.get("content");
    }

    private List<Cost> parseCosts(String aiContent, Budget budget) {
        String json = aiContent.trim();
        if (json.startsWith("```")) {
            json = json.replaceAll("^```(json)?\\s*", "").replaceAll("```\\s*$", "");
        }

        AiCostListResponse aiResponse;
        try {
            aiResponse = objectMapper.readValue(json, AiCostListResponse.class);
        } catch (Exception e) {
            throw new NoCostsFoundException("Couldn't find any costs");
        }

        if (aiResponse.costs() == null) {
            return List.of();
        }

        return aiResponse.costs().stream()
                .filter(dto -> dto.name() != null && !dto.name().isBlank() && dto.amount() != null && dto.amount() >= 0)
                .map(dto -> {
                    Cost cost = new Cost();
                    cost.setName(dto.name().trim());
                    cost.setAmount(Math.round(dto.amount() * 100) / 100.0);
                    cost.setBudget(budget);
                    return cost;
                })
                .limit(1)
                .toList();
    }
}
