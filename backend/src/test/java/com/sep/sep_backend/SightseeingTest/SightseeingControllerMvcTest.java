package com.sep.sep_backend.SightseeingTest;

import com.sep.sep_backend.SighseeingSugesstion.SightseeingController;
import com.sep.sep_backend.SighseeingSugesstion.SightseeingService;
import com.sep.sep_backend.auth.JwtService;
import com.sep.sep_backend.config.SecurityConfig;
import com.sep.sep_backend.cost.Cost;
import com.sep.sep_backend.registration.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = SightseeingController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
public class SightseeingControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    // Diese Mocks werden benötigt, da Spring sie im ApplicationContext für die Security-Konfiguration sucht
    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AppUserRepository appUserRepository;

    @MockitoBean
    private SightseeingService sightseeingService;

    // --- GET /api/sightseeing/suggestions/{travelEntryId} ---

    @Test
    void getSuggestions_shouldReturn200AndSuggestionsList() throws Exception {
        // Arrange
        UUID travelEntryId = UUID.randomUUID();

        Cost sight1 = new Cost();
        sight1.setName("Eiffelturm");
        sight1.setAmount(25.0);

        Cost sight2 = new Cost();
        sight2.setName("Louvre");
        sight2.setAmount(17.5);

        when(sightseeingService.getTopThreeSightsForTravelEntry(travelEntryId))
                .thenReturn(List.of(sight1, sight2));

        // Act & Assert
        mockMvc.perform(get("/api/sightseeing/suggestions/" + travelEntryId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Eiffelturm"))
                .andExpect(jsonPath("$[0].amount").value(25.0))
                .andExpect(jsonPath("$[1].name").value("Louvre"))
                .andExpect(jsonPath("$[1].amount").value(17.5));

        verify(sightseeingService).getTopThreeSightsForTravelEntry(travelEntryId);
    }

    @Test
    void getSuggestions_shouldReturn400WhenIllegalArgumentExceptionThrown() throws Exception {
        // Arrange
        UUID travelEntryId = UUID.randomUUID();
        String exceptionMessage = "Budget mit ID " + travelEntryId + " nicht gefunden";

        when(sightseeingService.getTopThreeSightsForTravelEntry(travelEntryId))
                .thenThrow(new IllegalArgumentException(exceptionMessage));

        // Act & Assert
        mockMvc.perform(get("/api/sightseeing/suggestions/" + travelEntryId))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(exceptionMessage));

        verify(sightseeingService).getTopThreeSightsForTravelEntry(travelEntryId);
    }

    @Test
    void getSuggestions_shouldReturn400WhenIllegalStateExceptionThrown() throws Exception {
        // Arrange
        UUID travelEntryId = UUID.randomUUID();
        String exceptionMessage = "Dieses Budget ist mit keinem Reiseeintrag verknüpft.";

        when(sightseeingService.getTopThreeSightsForTravelEntry(travelEntryId))
                .thenThrow(new IllegalStateException(exceptionMessage));

        // Act & Assert
        mockMvc.perform(get("/api/sightseeing/suggestions/" + travelEntryId))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(exceptionMessage));

        verify(sightseeingService).getTopThreeSightsForTravelEntry(travelEntryId);
    }

    @Test
    void getSuggestions_shouldReturn500WhenGenericExceptionThrown() throws Exception {
        // Arrange
        UUID travelEntryId = UUID.randomUUID();
        String internalError = "Verbindungsfehler zur Groq-API";

        when(sightseeingService.getTopThreeSightsForTravelEntry(travelEntryId))
                .thenThrow(new RuntimeException(internalError));

        // Act & Assert
        mockMvc.perform(get("/api/sightseeing/suggestions/" + travelEntryId))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Fehler bei der Kommunikation mit der KI: " + internalError));

        verify(sightseeingService).getTopThreeSightsForTravelEntry(travelEntryId);
    }
}