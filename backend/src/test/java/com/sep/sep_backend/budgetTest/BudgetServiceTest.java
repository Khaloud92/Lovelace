package com.sep.sep_backend.budgetTest;

import com.sep.sep_backend.budget.Budget;
import com.sep.sep_backend.budget.BudgetRepository;
import com.sep.sep_backend.budget.BudgetService;
import com.sep.sep_backend.budget.dto.request.CreateBudgetRequest;
import com.sep.sep_backend.budget.dto.request.UpdateBudgetRequest;
import com.sep.sep_backend.budget.dto.response.BudgetResponse;
import com.sep.sep_backend.travelentry.TravelEntry;
import com.sep.sep_backend.travelentry.TravelEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepo;

    @Mock
    private TravelEntryRepository travelEntryRepo;

    @InjectMocks
    private BudgetService budgetService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // --- getAllAsResponse ---

    @Test
    void getAllAsResponse_returnsMappedBudgetResponses() {
        // GIVEN
        String userId = "00000000-0000-0000-0000-000000000001";

        TravelEntry travelEntry = new TravelEntry();
        travelEntry.setId(UUID.randomUUID());
        travelEntry.setStartDate(LocalDate.of(2025, 6, 1));
        travelEntry.setEndDate(LocalDate.of(2025, 6, 14));
        travelEntry.setColor("#FF5733");

        Budget budget = new Budget();
        budget.setId(UUID.randomUUID());
        budget.setName("Sommerurlaub");
        budget.setAmount(800.0);
        budget.setTravelEntry(travelEntry);

        when(budgetRepo.findByTravelEntry_UserId(userId)).thenReturn(List.of(budget));

        // WHEN
        List<BudgetResponse> result = budgetService.getAllAsResponse(userId);

        // THEN
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(budget.getId());
        assertThat(result.get(0).getName()).isEqualTo("Sommerurlaub");
        assertThat(result.get(0).getAmount()).isEqualTo(800.0);
        assertThat(result.get(0).getTravelEntry()).isNotNull();
        assertThat(result.get(0).getTravelEntry().getColor()).isEqualTo("#FF5733");

        verify(budgetRepo).findByTravelEntry_UserId(userId);
    }

    @Test
    void getAllAsResponse_withNullTravelEntry_returnsBudgetResponseWithNullTravelEntry() {
        // GIVEN
        String userId = "00000000-0000-0000-0000-000000000001";

        Budget budget = new Budget();
        budget.setId(UUID.randomUUID());
        budget.setName("Budget ohne Reise");
        budget.setAmount(200.0);
        budget.setTravelEntry(null);

        when(budgetRepo.findByTravelEntry_UserId(userId)).thenReturn(List.of(budget));

        // WHEN
        List<BudgetResponse> result = budgetService.getAllAsResponse(userId);

        // THEN
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTravelEntry()).isNull();
    }

    // --- create ---

    @Test
    void create_savesBudget() {
        UUID travelEntryid = UUID.randomUUID();

        TravelEntry mockEntry = new TravelEntry();
        mockEntry.setId(travelEntryid);
        when(travelEntryRepo.findById(travelEntryid)).thenReturn(Optional.of(mockEntry));

        CreateBudgetRequest request = new CreateBudgetRequest();
        request.setName("New");
        request.setAmount(1000.0);

        Budget savedBudget = new Budget();
        savedBudget.setName("New");
        savedBudget.setAmount(1000.0);

        when(budgetRepo.save(any(Budget.class))).thenReturn(savedBudget);

        // WHEN
        Budget result = budgetService.create(travelEntryid, request);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("New");
        assertThat(result.getAmount()).isEqualTo(1000.0);

        verify(travelEntryRepo).findById(travelEntryid);
        verify(budgetRepo).save(any(Budget.class));
    }

    @Test
    void create_throwsException_whenAmountIsNegative() {
        // GIVEN
        UUID travelEntryId = UUID.randomUUID();
        TravelEntry mockEntry = new TravelEntry();
        when(travelEntryRepo.findById(travelEntryId)).thenReturn(Optional.of(mockEntry));

        CreateBudgetRequest request = new CreateBudgetRequest();
        request.setName("Test");
        request.setAmount(-50.0);

        // WHEN & THEN
        assertThatThrownBy(() -> budgetService.create(travelEntryId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Please enter a positive Budget amount");
    }

    @Test
    void create_throwsException_whenNameIsNull() {
        // GIVEN
        UUID travelEntryId = UUID.randomUUID();
        TravelEntry mockEntry = new TravelEntry();
        when(travelEntryRepo.findById(travelEntryId)).thenReturn(Optional.of(mockEntry));

        CreateBudgetRequest request = new CreateBudgetRequest();
        request.setName(null);
        request.setAmount(100.0);

        // WHEN & THEN
        assertThatThrownBy(() -> budgetService.create(travelEntryId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Please enter a name");
    }

    // --- update ---

    @Test
    void update_existingId_updatesAndReturnsBudget() {
        UUID id = UUID.randomUUID();

        UpdateBudgetRequest request = new UpdateBudgetRequest();
        request.setName("  Neuer Name  ");
        request.setAmount(100.0);

        Budget existingBudget = new Budget();
        existingBudget.setId(id);
        existingBudget.setName("Alter Name");
        existingBudget.setAmount(50.0);

        when(budgetRepo.findById(id)).thenReturn(Optional.of(existingBudget));
        when(budgetRepo.save(any(Budget.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Budget result = budgetService.update(id, request);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Neuer Name");
        assertThat(result.getAmount()).isEqualTo(100.0);

        verify(budgetRepo).findById(id);
        verify(budgetRepo).save(existingBudget);
    }

    @Test
    void update_nonExistingId_throwsException() {
        UUID id = UUID.randomUUID();
        UpdateBudgetRequest request = new UpdateBudgetRequest();
        request.setName("Test");
        request.setAmount(10.0);

        when(budgetRepo.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetService.update(id, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Budget not found");
    }

    // --- delete ---

    @Test
    void delete_callsRepository() {
        UUID id = UUID.randomUUID();
        budgetService.delete(id);
        verify(budgetRepo).deleteById(id);
    }
}