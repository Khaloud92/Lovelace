package com.sep.sep_backend.calendarTest;

import com.sep.sep_backend.calendar.CalendarEntry;
import com.sep.sep_backend.calendar.CalendarEntryRepository;
import com.sep.sep_backend.calendar.CalendarEntryService;
import com.sep.sep_backend.calendar.dto.CalendarEntryResponse;
import com.sep.sep_backend.calendar.dto.CreateCalendarEntryRequest;
import com.sep.sep_backend.calendar.dto.UpdateCalendarEntryRequest;
import com.sep.sep_backend.travelentry.TravelEntry;
import com.sep.sep_backend.travelentry.TravelEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalendarEntryServiceTest {

    @Mock private CalendarEntryRepository repository;
    @Mock private TravelEntryRepository travelEntryRepository;
    private CalendarEntryService service;

    @BeforeEach
    void setUp() { service = new CalendarEntryService(repository, travelEntryRepository); }

    @Test
    void getAllEntries_shouldReturnAllEntries() {
        CalendarEntry entry = new CalendarEntry();
        entry.setTitle("Test");
        when(repository.findByUserId("user-1")).thenReturn(List.of(entry));
        List<CalendarEntryResponse> result = service.getAllEntries("user-1");
        assertEquals(1, result.size());
        assertEquals("Test", result.get(0).getTitle());
    }

    @Test
    void getAllEntries_shouldReturnEmptyList_whenNoEntries() {
        when(repository.findByUserId("user-1")).thenReturn(List.of());
        assertTrue(service.getAllEntries("user-1").isEmpty());
    }

    @Test
    void createEntry_shouldSaveValidEntry() {
        CreateCalendarEntryRequest request = new CreateCalendarEntryRequest();
        request.setTitle("Doctor appointment");
        request.setStartDate(ZonedDateTime.parse("2026-05-11T09:30:00+02:00"));
        request.setEndDate(ZonedDateTime.parse("2026-05-11T10:30:00+02:00"));
        request.setLocation("Essen");
        request.setDescription("Checkup");
        request.setColor("#FF5733");

        CalendarEntry savedEntry = new CalendarEntry();
        savedEntry.setTitle(request.getTitle());
        savedEntry.setStartDate(request.getStartDate());
        savedEntry.setEndDate(request.getEndDate());
        savedEntry.setLocation(request.getLocation());
        savedEntry.setDescription(request.getDescription());
        savedEntry.setColor(request.getColor());
        when(repository.save(any(CalendarEntry.class))).thenReturn(savedEntry);

        CalendarEntryResponse result = service.createEntry(request, "user-1");

        assertNotNull(result);
        assertEquals("Doctor appointment", result.getTitle());
        assertEquals("Essen", result.getLocation());
        assertEquals("#FF5733", result.getColor());

        ArgumentCaptor<CalendarEntry> captor = ArgumentCaptor.forClass(CalendarEntry.class);
        verify(repository).save(captor.capture());
        assertEquals("Doctor appointment", captor.getValue().getTitle());
        assertEquals("#FF5733", captor.getValue().getColor());
    }

    @Test
    void createEntry_shouldThrowException_whenEndDateBeforeStartDate() {
        CreateCalendarEntryRequest request = new CreateCalendarEntryRequest();
        request.setTitle("Invalid");
        request.setStartDate(ZonedDateTime.parse("2026-05-11T12:00:00+02:00"));
        request.setEndDate(ZonedDateTime.parse("2026-05-11T10:00:00+02:00"));
        request.setColor("#FF5733");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.createEntry(request, "user-1"));
        assertEquals("End date must not be before start date.", ex.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void updateEntry_shouldSaveUpdatedEntry() {
        UUID id = UUID.randomUUID();
        UpdateCalendarEntryRequest request = new UpdateCalendarEntryRequest();
        request.setTitle("Updated appointment");
        request.setStartDate(ZonedDateTime.parse("2026-05-11T11:00:00+02:00"));
        request.setEndDate(ZonedDateTime.parse("2026-05-11T12:00:00+02:00"));
        request.setLocation("Essen");
        request.setDescription("Updated checkup");
        request.setColor("#123456");

        CalendarEntry existingEntry = new CalendarEntry();
        existingEntry.setId(id);
        existingEntry.setTitle("Old title");
        existingEntry.setStartDate(ZonedDateTime.parse("2026-05-11T09:30:00+02:00"));

        when(repository.findById(id)).thenReturn(Optional.of(existingEntry));
        when(repository.save(any(CalendarEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        CalendarEntryResponse result = service.updateEntry(id, request);

        assertNotNull(result);
        assertEquals("Updated appointment", result.getTitle());
        assertEquals("Essen", result.getLocation());
        assertEquals("#123456", result.getColor());
    }

    @Test
    void updateEntry_shouldThrowException_whenEntryDoesNotExist() {
        UUID id = UUID.randomUUID();
        UpdateCalendarEntryRequest request = new UpdateCalendarEntryRequest();
        request.setTitle("Test");
        request.setStartDate(ZonedDateTime.parse("2026-05-11T11:00:00+02:00"));
        request.setColor("#123456");
        when(repository.findById(id)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateEntry(id, request));
        assertEquals("Calendar entry not found.", ex.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void updateEntry_shouldThrowException_whenEndDateBeforeStartDate() {
        UUID id = UUID.randomUUID();
        UpdateCalendarEntryRequest request = new UpdateCalendarEntryRequest();
        request.setTitle("Invalid");
        request.setStartDate(ZonedDateTime.parse("2026-05-11T12:00:00+02:00"));
        request.setEndDate(ZonedDateTime.parse("2026-05-11T10:00:00+02:00"));
        request.setColor("#FF5733");

        CalendarEntry existingEntry = new CalendarEntry();
        existingEntry.setId(id);
        when(repository.findById(id)).thenReturn(Optional.of(existingEntry));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateEntry(id, request));
        assertEquals("End date must not be before start date.", ex.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void deleteEntry_shouldDeleteExistingEntry() {
        UUID id = UUID.randomUUID();
        CalendarEntry entry = new CalendarEntry();
        entry.setId(id);
        entry.setTravelEntry(null);
        entry.setAutoGenerated(false);
        when(repository.findById(id)).thenReturn(Optional.of(entry));

        service.deleteEntry(id);

        verify(repository).delete(entry);
        verify(travelEntryRepository, never()).deleteById(any());
    }

    @Test
    void deleteEntry_shouldDeleteTravelEntry_whenLinkedAndAutoGenerated() {
        UUID calendarId = UUID.randomUUID();
        UUID travelId = UUID.randomUUID();
        TravelEntry travelEntry = new TravelEntry();
        travelEntry.setId(travelId);

        CalendarEntry entry = new CalendarEntry();
        entry.setId(calendarId);
        entry.setTravelEntry(travelEntry);
        entry.setAutoGenerated(true); // Fix: entscheidend für die neue Logik

        when(repository.findById(calendarId)).thenReturn(Optional.of(entry));

        service.deleteEntry(calendarId);

        verify(travelEntryRepository).deleteById(travelId);
        verify(repository, never()).delete(any());
    }

    @Test
    void deleteEntry_shouldDeleteOnlyCalendarEntry_whenLinkedButManuallyCreated() {
        UUID calendarId = UUID.randomUUID();
        UUID travelId = UUID.randomUUID();
        TravelEntry travelEntry = new TravelEntry();
        travelEntry.setId(travelId);

        CalendarEntry entry = new CalendarEntry();
        entry.setId(calendarId);
        entry.setTravelEntry(travelEntry);
        entry.setAutoGenerated(false); // manuell erstellt, aber verlinkt

        when(repository.findById(calendarId)).thenReturn(Optional.of(entry));

        service.deleteEntry(calendarId);

        verify(repository).delete(entry);
        verify(travelEntryRepository, never()).deleteById(any());
    }

    @Test
    void deleteEntry_shouldThrowException_whenEntryDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.deleteEntry(id));

        assertEquals("Calendar entry not found.", ex.getMessage());
        verify(repository, never()).delete(any());
    }
}