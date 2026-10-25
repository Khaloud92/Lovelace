package com.sep.sep_backend.SightseeingTest;

import com.sep.sep_backend.SighseeingSugesstion.SightseeingController;
import com.sep.sep_backend.SighseeingSugesstion.SightseeingService;
import com.sep.sep_backend.cost.Cost;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SightseeingControllertest {

    @Mock
    private SightseeingService sightseeingService;

    @InjectMocks
    private SightseeingController controller;

    // --- GET /api/sightseeing/suggestions/{travelEntryId} ---

    @Test
    void getSuggestions_shouldReturn200WithSuggestionsList() throws Exception {
        // Arrange
        UUID travelEntryId = UUID.randomUUID();
        Cost sight1 = new Cost();
        Cost sight2 = new Cost();
        Cost sight3 = new Cost();
        List<Cost> mockSuggestions = List.of(sight1, sight2, sight3);

        when(sightseeingService.getTopThreeSightsForTravelEntry(travelEntryId)).thenReturn(mockSuggestions);

        // Act
        ResponseEntity<?> response = controller.getSuggestions(travelEntryId);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof List);
        assertEquals(3, ((List<?>) response.getBody()).size());
        verify(sightseeingService).getTopThreeSightsForTravelEntry(travelEntryId);
    }

    @Test
    void getSuggestions_shouldReturn400WhenBudgetNotFound() throws Exception {
        // Arrange
        UUID travelEntryId = UUID.randomUUID();
        String errorMessage = "Budget mit ID " + travelEntryId + " nicht gefunden";

        when(sightseeingService.getTopThreeSightsForTravelEntry(travelEntryId))
                .thenThrow(new IllegalArgumentException(errorMessage));

        // Act
        ResponseEntity<?> response = controller.getSuggestions(travelEntryId);

        // Assert
        assertEquals(400, response.getStatusCode().value());
        assertEquals(errorMessage, response.getBody());
        verify(sightseeingService).getTopThreeSightsForTravelEntry(travelEntryId);
    }

    @Test
    void getSuggestions_shouldReturn400WhenTravelEntryMissing() throws Exception {
        // Arrange
        UUID travelEntryId = UUID.randomUUID();
        String errorMessage = "Dieses Budget ist mit keinem Reiseeintrag verknüpft.";

        when(sightseeingService.getTopThreeSightsForTravelEntry(travelEntryId))
                .thenThrow(new IllegalStateException(errorMessage));

        // Act
        ResponseEntity<?> response = controller.getSuggestions(travelEntryId);

        // Assert
        assertEquals(400, response.getStatusCode().value());
        assertEquals(errorMessage, response.getBody());
        verify(sightseeingService).getTopThreeSightsForTravelEntry(travelEntryId);
    }

    @Test
    void getSuggestions_shouldReturn500WhenAiCommunicationFails() throws Exception {
        // Arrange
        UUID travelEntryId = UUID.randomUUID();
        String internalError = "Groq API timeout oder JSON-Parsing-Fehler";

        // Simuliert eine generische Exception (z.B. vom RestTemplate oder ObjectMapper)
        when(sightseeingService.getTopThreeSightsForTravelEntry(travelEntryId))
                .thenThrow(new RuntimeException(internalError));

        // Act
        ResponseEntity<?> response = controller.getSuggestions(travelEntryId);

        // Assert
        assertEquals(500, response.getStatusCode().value());
        assertEquals("Fehler bei der Kommunikation mit der KI: " + internalError, response.getBody());
        verify(sightseeingService).getTopThreeSightsForTravelEntry(travelEntryId);
    }
}
