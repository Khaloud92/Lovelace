package com.sep.sep_backend.calendarTest;

import com.sep.sep_backend.calendar.CalendarEntryController;
import com.sep.sep_backend.calendar.CalendarEntryService;
import com.sep.sep_backend.calendar.DocumentScannerService;
import com.sep.sep_backend.calendar.dto.CalendarEntryResponse;
import com.sep.sep_backend.auth.JwtService;
import com.sep.sep_backend.calendar.dto.CreateCalendarEntryRequest;
import com.sep.sep_backend.calendar.dto.UpdateCalendarEntryRequest;
import com.sep.sep_backend.registration.AppUser;
import com.sep.sep_backend.registration.AppUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import com.sep.sep_backend.config.SecurityConfig;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


//Problems with jwt security Configuration

@WebMvcTest(value = CalendarEntryController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))

class CalendarEntryControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AppUserRepository appUserRepository;

    @MockitoBean
    private CalendarEntryService service;

    @MockitoBean
    private DocumentScannerService documentScannerService;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_USER_ID = "00000000-0000-0000-0000-000000000001";

    @BeforeEach
    void setUpSecurityContext() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(TEST_EMAIL, null, List.of()));
        SecurityContextHolder.setContext(context);

        AppUser testUser = new AppUser();
        testUser.setId(UUID.fromString(TEST_USER_ID));
        lenient().when(appUserRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAllEntries_shouldReturn200() throws Exception {
        CalendarEntryResponse entry = new CalendarEntryResponse();
        entry.setTitle("Test");
        when(service.getAllEntries(TEST_USER_ID)).thenReturn(List.of(entry));

        mockMvc.perform(get("/api/calendar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test"));
    }

    @Test
    void getAllEntries_shouldReturnEmptyList() throws Exception {
        when(service.getAllEntries(TEST_USER_ID)).thenReturn(List.of());

        mockMvc.perform(get("/api/calendar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void createEntry_shouldReturn201() throws Exception {
        CalendarEntryResponse createdEntry = new CalendarEntryResponse();
        createdEntry.setId(UUID.randomUUID());
        createdEntry.setTitle("Doctor appointment");
        createdEntry.setStartDate(ZonedDateTime.parse("2026-05-11T09:30:00+02:00"));
        createdEntry.setColor("#FF5733");

        when(service.createEntry(any(CreateCalendarEntryRequest.class), any())).thenReturn(createdEntry);

        String requestBody = """
            {
              "title": "Doctor appointment",
              "startDate": "2026-05-11T09:30:00+02:00",
              "color": "#FF5733"
            }
            """;

        mockMvc.perform(post("/api/calendar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Doctor appointment"))
                .andExpect(jsonPath("$.color").value("#FF5733"));
    }

    @Test
    void createEntry_shouldReturn400_whenBodyIsEmpty() throws Exception {
        mockMvc.perform(post("/api/calendar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createEntry_shouldReturn400_whenTitleIsMissing() throws Exception {
        String requestBody = """
            {
              "startDate": "2026-05-11T09:30:00+02:00",
              "color": "#FF5733"
            }
            """;

        mockMvc.perform(post("/api/calendar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createEntry_shouldReturn400_whenStartDateIsMissing() throws Exception {
        String requestBody = """
            {
              "title": "Doctor appointment",
              "color": "#FF5733"
            }
            """;

        mockMvc.perform(post("/api/calendar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createEntry_shouldReturn400_whenColorIsMissing() throws Exception {
        String requestBody = """
        {
          "title": "Doctor appointment",
          "startDate": "2026-05-11T09:30:00+02:00"
        }
        """;

        mockMvc.perform(post("/api/calendar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateEntry_shouldReturn200() throws Exception {
        UUID id = UUID.randomUUID();

        CalendarEntryResponse updatedEntry = new CalendarEntryResponse();
        updatedEntry.setId(id);
        updatedEntry.setTitle("Updated appointment");
        updatedEntry.setStartDate(ZonedDateTime.parse("2026-05-11T11:00:00+02:00"));
        updatedEntry.setColor("#123456");

        when(service.updateEntry(any(UUID.class), any(UpdateCalendarEntryRequest.class))).thenReturn(updatedEntry);

        String requestBody = """
        {
          "title": "Updated appointment",
          "startDate": "2026-05-11T11:00:00+02:00",
          "color": "#123456"
        }
        """;

        mockMvc.perform(put("/api/calendar/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Updated appointment"))
                .andExpect(jsonPath("$.color").value("#123456"));
    }

    @Test
    void updateEntry_shouldReturn400_whenTitleIsMissing() throws Exception {
        UUID id = UUID.randomUUID();

        String requestBody = """
        {
          "startDate": "2026-05-11T11:00:00+02:00",
          "color": "#123456"
        }
        """;

        mockMvc.perform(put("/api/calendar/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateEntry_shouldReturn400_whenStartDateIsMissing() throws Exception {
        UUID id = UUID.randomUUID();

        String requestBody = """
        {
          "title": "Updated appointment",
          "color": "#123456"
        }
        """;

        mockMvc.perform(put("/api/calendar/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateEntry_shouldReturn400_whenColorIsMissing() throws Exception {
        UUID id = UUID.randomUUID();

        String requestBody = """
        {
          "title": "Updated appointment",
          "startDate": "2026-05-11T11:00:00+02:00"
        }
        """;

        mockMvc.perform(put("/api/calendar/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteEntry_shouldReturn204() throws Exception {
        UUID id = UUID.randomUUID();

        doNothing().when(service).deleteEntry(id);

        mockMvc.perform(delete("/api/calendar/" + id))
                .andExpect(status().isNoContent());

        verify(service).deleteEntry(id);
    }
}
