package com.sep.sep_backend.calendar;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;

/**
 * Extracts trip data (title, dates, location) from ticket PDFs via the Groq API.
 */
@Service
public class DocumentScannerService {

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Value("${groq.api.url}")
    private String groqUrl;

    private final RestTemplate restTemplate;

    public DocumentScannerService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Reads the PDF text and asks the model for a JSON object with title, dates, and location.
     *
     * @param file the ticket PDF
     * @return the model's JSON answer as a string
     */
    public String scanAndExtractData(MultipartFile file) throws Exception {
        String extractedText = "";
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            extractedText = stripper.getText(document);
        }

        String prompt = "Du bist ein Reiseassistent. Lese diesen Text: " + extractedText +
                " Antworte AUSSCHLIESSLICH mit einem validen JSON-Objekt. " +
                "WICHTIG: startDate und endDate MÜSSEN im Format YYYY-MM-DDTHH:mm:ssZ sein (mit dem Z am Ende!). " +
                "Keys: title, startDate, endDate, location. Kein Markdown!";

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

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> firstChoice = choices.get(0);
        Map<String, Object> responseMessage = (Map<String, Object>) firstChoice.get("message");

        return (String) responseMessage.get("content");
    }
}