package com.sep.sep_backend.calendarTest;

import com.sep.sep_backend.calendar.CalendarEntryController;
import com.sep.sep_backend.calendar.CalendarEntryService;
import com.sep.sep_backend.calendar.DocumentScannerService;
import com.sep.sep_backend.calendar.dto.CalendarEntryResponse;
import com.sep.sep_backend.calendar.dto.CreateCalendarEntryRequest;
import com.sep.sep_backend.calendar.dto.UpdateCalendarEntryRequest;
import com.sep.sep_backend.registration.AppUser;
import com.sep.sep_backend.registration.AppUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalendarEntryControllerTest {

    @Mock private CalendarEntryService service;
    @Mock private DocumentScannerService documentScannerService;
    @Mock private AppUserRepository appUserRepository;
    @InjectMocks private CalendarEntryController controller;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_USER_ID = "00000000-0000-0000-0000-000000000001";

    @BeforeEach
    void setUpSecurityContext() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(TEST_EMAIL, null, List.of()));
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void clearSecurityContext() { SecurityContextHolder.clearContext(); }

    private void stubUserLookup() {
        AppUser testUser = new AppUser();
        testUser.setId(UUID.fromString(TEST_USER_ID));
        when(appUserRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
    }

    @Test
    void getAllEntries_shouldReturnAllEntries() {
        stubUserLookup();
        CalendarEntryResponse entry = new CalendarEntryResponse();
        entry.setTitle("Test");
        when(service.getAllEntries(TEST_USER_ID)).thenReturn(List.of(entry));

        ResponseEntity<List<CalendarEntryResponse>> response = controller.getAllEntries();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        assertEquals("Test", response.getBody().get(0).getTitle());
    }

    @Test
    void getAllEntries_shouldReturnEmptyList() {
        stubUserLookup();
        when(service.getAllEntries(TEST_USER_ID)).thenReturn(List.of());
        ResponseEntity<List<CalendarEntryResponse>> response = controller.getAllEntries();
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void createEntry_shouldReturn201WithCreatedEntry() {
        stubUserLookup();
        CreateCalendarEntryRequest request = new CreateCalendarEntryRequest();
        request.setTitle("Doctor appointment");
        request.setStartDate(ZonedDateTime.parse("2026-05-11T09:30:00+02:00"));
        request.setColor("#FF5733");

        CalendarEntryResponse createdEntry = new CalendarEntryResponse();
        createdEntry.setTitle("Doctor appointment");
        createdEntry.setColor("#FF5733");

        when(service.createEntry(any(CreateCalendarEntryRequest.class), eq(TEST_USER_ID)))
                .thenReturn(createdEntry);

        ResponseEntity<CalendarEntryResponse> response = controller.createEntry(request);

        assertEquals(201, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Doctor appointment", response.getBody().getTitle());
        assertEquals("#FF5733", response.getBody().getColor());
        verify(service).createEntry(any(CreateCalendarEntryRequest.class), eq(TEST_USER_ID));
    }

    @Test
    void updateEntry_shouldReturn200WithUpdatedEntry() {
        UUID id = UUID.randomUUID();
        UpdateCalendarEntryRequest request = new UpdateCalendarEntryRequest();
        request.setTitle("Updated appointment");
        request.setStartDate(ZonedDateTime.parse("2026-05-11T11:00:00+02:00"));
        request.setColor("#123456");

        CalendarEntryResponse updatedEntry = new CalendarEntryResponse();
        updatedEntry.setId(id);
        updatedEntry.setTitle("Updated appointment");
        updatedEntry.setColor("#123456");

        when(service.updateEntry(id, request)).thenReturn(updatedEntry);
        ResponseEntity<CalendarEntryResponse> response = controller.updateEntry(id, request);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Updated appointment", response.getBody().getTitle());
        verify(service).updateEntry(id, request);
    }

    @Test
    void deleteEntry_shouldReturn204() {
        UUID id = UUID.randomUUID();
        ResponseEntity<Void> response = controller.deleteEntry(id);
        assertEquals(204, response.getStatusCode().value());
        assertNull(response.getBody());
        verify(service).deleteEntry(id);
    }
}