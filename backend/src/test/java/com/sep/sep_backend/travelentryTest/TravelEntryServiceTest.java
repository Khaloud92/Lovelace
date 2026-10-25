package com.sep.sep_backend.travelentryTest;

import com.sep.sep_backend.budget.Budget;
import com.sep.sep_backend.budget.BudgetService;
import com.sep.sep_backend.budget.dto.request.CreateBudgetRequest;
import com.sep.sep_backend.calendar.CalendarEntryRepository;
import com.sep.sep_backend.travelentry.TravelEntry;
import com.sep.sep_backend.travelentry.TravelEntryRepository;
import com.sep.sep_backend.travelentry.TravelEntryService;
import com.sep.sep_backend.travelentry.dto.CreateTravelEntryrequest;
import com.sep.sep_backend.travelentry.dto.TravelEntryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TravelEntryServiceTest {

    @Mock
    private TravelEntryRepository repository;

    @Mock
    private BudgetService budgetService;

    @Mock
    private CalendarEntryRepository calendarEntryRepository;

    private TravelEntryService service;

    @BeforeEach
    void setUp() {
        service = new TravelEntryService(repository, budgetService, calendarEntryRepository);
    }

    // ══════════════════════════════════════════════════════════
    // findAll
    // ══════════════════════════════════════════════════════════

    @Test
    void findAll_shouldReturnAllEntriesForUser() {
        TravelEntry entry = createValidEntry();
        entry.setId(UUID.randomUUID());
        when(repository.findByUserId("user-1")).thenReturn(List.of(entry));

        List<TravelEntryResponse> result = service.findAll("user-1");

        assertEquals(1, result.size());
        assertEquals("Paris Trip", result.get(0).getTitle());
    }

    @Test
    void findAll_shouldReturnEmptyList_whenNoEntries() {
        when(repository.findByUserId("user-1")).thenReturn(List.of());
        assertTrue(service.findAll("user-1").isEmpty());
    }

    // ══════════════════════════════════════════════════════════
    // create
    // ══════════════════════════════════════════════════════════

    @Test
    void createTravelEntryShouldSaveAndThenCreateBudget() {
        UUID generatedId = UUID.randomUUID();

        CreateTravelEntryrequest request = new CreateTravelEntryrequest();
        request.setTitle("Paris Trip");
        request.setDestination("Paris, France");
        request.setStartDate(LocalDate.of(2026, 6, 1));
        request.setEndDate(LocalDate.of(2026, 6, 7));
        request.setUserId("user-1");

        CreateBudgetRequest budgetRequest = new CreateBudgetRequest();
        request.setBudget(budgetRequest);

        when(repository.save(any(TravelEntry.class))).thenAnswer(invocation -> {
            TravelEntry saved = invocation.getArgument(0);
            saved.setId(generatedId);
            return saved;
        });

        TravelEntryResponse result = service.create(request);

        assertEquals(generatedId, result.getId());
        assertEquals("Paris Trip", result.getTitle());

        verify(budgetService).create(generatedId, budgetRequest);
        verify(repository).save(any(TravelEntry.class));
    }

    @Test
    void createTravelEntry_shouldNotCallBudgetService_whenNoBudgetProvided() {
        UUID generatedId = UUID.randomUUID();

        CreateTravelEntryrequest request = new CreateTravelEntryrequest();
        request.setTitle("Berlin Trip");
        request.setDestination("Berlin");
        request.setStartDate(LocalDate.of(2026, 6, 1));
        request.setEndDate(LocalDate.of(2026, 6, 7));
        request.setUserId("user-1");

        when(repository.save(any(TravelEntry.class))).thenAnswer(invocation -> {
            TravelEntry saved = invocation.getArgument(0);
            saved.setId(generatedId);
            return saved;
        });

        service.create(request);

        verify(budgetService, never()).create(any(), any());
    }

    @Test
    void createTravelEntry_shouldUseDefaultColor_whenNoneProvided() {
        UUID generatedId = UUID.randomUUID();
        CreateTravelEntryrequest request = new CreateTravelEntryrequest();
        request.setTitle("Rom Trip");
        request.setDestination("Rom");
        request.setStartDate(LocalDate.of(2026, 6, 1));
        request.setEndDate(LocalDate.of(2026, 6, 7));
        request.setUserId("user-1");

        when(repository.save(any(TravelEntry.class))).thenAnswer(invocation -> {
            TravelEntry saved = invocation.getArgument(0);
            saved.setId(generatedId);
            return saved;
        });

        TravelEntryResponse result = service.create(request);

        assertEquals("#10b981", result.getColor());
    }

    @Test
    void createTravelEntry_shouldThrowException_whenEndDateBeforeStartDate() {
        CreateTravelEntryrequest request = new CreateTravelEntryrequest();
        request.setTitle("Invalid Trip");
        request.setDestination("Nowhere");
        request.setStartDate(LocalDate.of(2026, 6, 10));
        request.setEndDate(LocalDate.of(2026, 6, 1));
        request.setUserId("user-1");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.create(request));
        assertEquals("End date must be after start date", ex.getMessage());
        verify(repository, never()).save(any());
    }

    // ══════════════════════════════════════════════════════════
    // update
    // ══════════════════════════════════════════════════════════

    @Test
    void updateTravelEntryShouldUpdateAllFields() {
        UUID id = UUID.randomUUID();
        TravelEntry existingEntry = createValidEntry();
        existingEntry.setId(id);

        TravelEntry updateData = new TravelEntry();
        updateData.setTitle("New Title");
        updateData.setDestination("New Destination");
        updateData.setStartDate(LocalDate.of(2026, 8, 1));
        updateData.setEndDate(LocalDate.of(2026, 8, 10));
        updateData.setUserId("user-1");

        when(repository.findById(id)).thenReturn(Optional.of(existingEntry));
        when(repository.save(any(TravelEntry.class))).thenAnswer(i -> i.getArgument(0));

        TravelEntryResponse result = service.update(id, updateData, "user-1");

        assertEquals("New Title", result.getTitle());
        assertEquals("New Destination", result.getDestination());
        verify(repository).save(existingEntry);
    }

    @Test
    void updateTravelEntry_shouldKeepColor_whenUpdateColorIsBlank() {
        UUID id = UUID.randomUUID();
        TravelEntry existingEntry = createValidEntry();
        existingEntry.setId(id);
        existingEntry.setColor("#123456");

        TravelEntry updateData = createValidEntry();
        updateData.setColor("");

        when(repository.findById(id)).thenReturn(Optional.of(existingEntry));
        when(repository.save(any(TravelEntry.class))).thenAnswer(i -> i.getArgument(0));

        TravelEntryResponse result = service.update(id, updateData, "user-1");

        assertEquals("#123456", result.getColor());
    }

    @Test
    void updateTravelEntry_shouldUpdateExistingBudget() {
        UUID id = UUID.randomUUID();
        TravelEntry existingEntry = createValidEntry();
        existingEntry.setId(id);

        Budget existingBudget = new Budget();
        existingBudget.setAmount(100.0);
        existingBudget.setName("Old Budget");
        existingEntry.setBudget(existingBudget);

        Budget incomingBudget = new Budget();
        incomingBudget.setAmount(999.0);
        incomingBudget.setName("New Budget");

        TravelEntry updateData = createValidEntry();
        updateData.setBudget(incomingBudget);

        when(repository.findById(id)).thenReturn(Optional.of(existingEntry));
        when(repository.save(any(TravelEntry.class))).thenAnswer(i -> i.getArgument(0));

        TravelEntryResponse result = service.update(id, updateData, "user-1");

        assertEquals(999.0, existingEntry.getBudget().getAmount());
        assertEquals("New Budget", existingEntry.getBudget().getName());
        assertNotNull(result);
    }

    @Test
    void updateTravelEntry_shouldAttachNewBudget_whenNoneExisted() {
        UUID id = UUID.randomUUID();
        TravelEntry existingEntry = createValidEntry();
        existingEntry.setId(id);
        existingEntry.setBudget(null);

        Budget incomingBudget = new Budget();
        incomingBudget.setAmount(500.0);
        incomingBudget.setName("Fresh Budget");

        TravelEntry updateData = createValidEntry();
        updateData.setBudget(incomingBudget);

        when(repository.findById(id)).thenReturn(Optional.of(existingEntry));
        when(repository.save(any(TravelEntry.class))).thenAnswer(i -> i.getArgument(0));

        service.update(id, updateData, "user-1");

        assertEquals(existingEntry, incomingBudget.getTravelEntry());
        assertEquals(incomingBudget, existingEntry.getBudget());
    }

    @Test
    void updateTravelEntry_shouldThrowException_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        TravelEntry updateData = createValidEntry();

        assertThrows(IllegalArgumentException.class, () -> service.update(id, updateData, "user-1"));
        verify(repository, never()).save(any());
    }

    @Test
    void updateTravelEntry_shouldThrowException_whenUserIdMismatch() {
        UUID id = UUID.randomUUID();
        TravelEntry existingEntry = createValidEntry();
        existingEntry.setId(id);
        existingEntry.setUserId("user-1");

        when(repository.findById(id)).thenReturn(Optional.of(existingEntry));

        TravelEntry updateData = createValidEntry();

        assertThrows(IllegalArgumentException.class, () -> service.update(id, updateData, "user-2"));
        verify(repository, never()).save(any());
    }

    @Test
    void updateTravelEntry_shouldThrowException_whenTitleMissing() {
        TravelEntry updateData = createValidEntry();
        updateData.setTitle("");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.update(UUID.randomUUID(), updateData, "user-1"));
        assertEquals("Title is required", ex.getMessage());
    }

    @Test
    void updateTravelEntry_shouldThrowException_whenDestinationMissing() {
        TravelEntry updateData = createValidEntry();
        updateData.setDestination(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.update(UUID.randomUUID(), updateData, "user-1"));
        assertEquals("Destination is required", ex.getMessage());
    }

    @Test
    void updateTravelEntry_shouldThrowException_whenDatesMissing() {
        TravelEntry updateData = createValidEntry();
        updateData.setStartDate(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.update(UUID.randomUUID(), updateData, "user-1"));
        assertEquals("Start date and end date are required", ex.getMessage());
    }

    @Test
    void updateTravelEntry_shouldThrowException_whenEndDateBeforeStartDate() {
        TravelEntry updateData = createValidEntry();
        updateData.setStartDate(LocalDate.of(2026, 8, 10));
        updateData.setEndDate(LocalDate.of(2026, 8, 1));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.update(UUID.randomUUID(), updateData, "user-1"));
        assertEquals("End date must be after start date", ex.getMessage());
    }

    // ══════════════════════════════════════════════════════════
    // delete
    // ══════════════════════════════════════════════════════════

    @Test
    void deleteShouldRemoveEntry_whenFoundAndUserMatches() {
        UUID id = UUID.randomUUID();
        TravelEntry entry = createValidEntry();
        entry.setId(id);
        entry.setUserId("user-1");

        when(repository.findById(id)).thenReturn(Optional.of(entry));

        service.delete(id, "user-1");

        verify(repository).deleteById(id);
    }

    @Test
    void deleteShouldThrowExceptionWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.delete(id, "user-1"));
        verify(repository, never()).deleteById(any());
    }

    @Test
    void deleteShouldThrowException_whenUserIdMismatch() {
        UUID id = UUID.randomUUID();
        TravelEntry entry = createValidEntry();
        entry.setId(id);
        entry.setUserId("user-1");

        when(repository.findById(id)).thenReturn(Optional.of(entry));

        assertThrows(IllegalArgumentException.class, () -> service.delete(id, "user-2"));
        verify(repository, never()).deleteById(any());
    }

    private TravelEntry createValidEntry() {
        TravelEntry entry = new TravelEntry();
        entry.setTitle("Paris Trip");
        entry.setDestination("Paris, France");
        entry.setStartDate(LocalDate.of(2026, 6, 1));
        entry.setEndDate(LocalDate.of(2026, 6, 7));
        entry.setUserId("user-1");
        return entry;
    }
}