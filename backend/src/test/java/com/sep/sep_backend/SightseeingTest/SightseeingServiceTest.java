package com.sep.sep_backend.SightseeingTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep.sep_backend.SighseeingSugesstion.SightseeingService;
import com.sep.sep_backend.budget.Budget;
import com.sep.sep_backend.budget.BudgetRepository;
import com.sep.sep_backend.cost.Cost;
import com.sep.sep_backend.travelentry.TravelEntry;
import com.sep.sep_backend.travelentry.TravelEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.http.HttpEntity;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SightseeingServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private TravelEntryRepository travelEntryRepository; // Im Konstruktor verlangt, obwohl ungenutzt

    @InjectMocks
    private SightseeingService sightseeingService;

    private static final String MOCK_URL = "https://api.groq.com/v1/chat/completions";
    private static final String MOCK_KEY = "mock-api-key";

    @BeforeEach
    void setUp() {
        // Injiziert die @Value-Felder manuell in das Mockito-Objekt
        ReflectionTestUtils.setField(sightseeingService, "groqUrl", MOCK_URL);
        ReflectionTestUtils.setField(sightseeingService, "groqApiKey", MOCK_KEY);
    }

    @Test
    void getTopThreeSightsForTravelEntry_shouldReturnListOfCostsOnSuccess() throws Exception {
        // Arrange
        UUID budgetId = UUID.randomUUID();

        TravelEntry travelEntry = new TravelEntry();
        travelEntry.setDestination("Paris");

        Budget budget = new Budget();
        budget.setId(budgetId);
        budget.setTravelEntry(travelEntry);

        // Simuliere DB-Aufruf
        when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(budget));

        // Simuliere die Antwort der Groq-API (verschachtelte Maps)
        String rawAiJson = "{\"sights\": [{\"name\": \"Eiffelturm\", \"amount\": 25.0}, {\"name\": \"Louvre\", \"amount\": 17.5}, {\"name\": \"Notre-Dame\", \"amount\": 0.0}]}";

        Map<String, Object> mockResponseMessage = Map.of("content", rawAiJson);
        Map<String, Object> mockChoice = Map.of("message", mockResponseMessage);
        Map<String, Object> mockApiResponse = Map.of("choices", List.of(mockChoice));

        when(restTemplate.postForObject(eq(MOCK_URL), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(mockApiResponse);

        // Act
        List<Cost> result = sightseeingService.getTopThreeSightsForTravelEntry(budgetId);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());

        assertEquals("Eiffelturm", result.get(0).getName());
        assertEquals(25.0, result.get(0).getAmount());
        assertEquals(budget, result.get(0).getBudget());

        assertEquals("Louvre", result.get(1).getName());
        assertEquals(17.5, result.get(1).getAmount());

        assertEquals("Notre-Dame", result.get(2).getName());
        assertEquals(0.0, result.get(2).getAmount());

        verify(budgetRepository).findById(budgetId);
        verify(restTemplate).postForObject(eq(MOCK_URL), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void getTopThreeSightsForTravelEntry_shouldThrowIllegalArgumentExceptionWhenBudgetNotFound() {
        // Arrange
        UUID budgetId = UUID.randomUUID();
        when(budgetRepository.findById(budgetId)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            sightseeingService.getTopThreeSightsForTravelEntry(budgetId);
        });

        assertTrue(exception.getMessage().contains("Budget mit ID"));
        verify(budgetRepository).findById(budgetId);
        verifyNoInteractions(restTemplate);
    }

    @Test
    void getTopThreeSightsForTravelEntry_shouldThrowIllegalStateExceptionWhenTravelEntryMissing() {
        // Arrange
        UUID budgetId = UUID.randomUUID();
        Budget budget = new Budget();
        budget.setId(budgetId);
        budget.setTravelEntry(null); // Kein Reiseeintrag verknüpft

        when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(budget));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            sightseeingService.getTopThreeSightsForTravelEntry(budgetId);
        });

        assertEquals("Dieses Budget ist mit keinem Reiseeintrag verknüpft.", exception.getMessage());
        verify(budgetRepository).findById(budgetId);
        verifyNoInteractions(restTemplate);
    }
}