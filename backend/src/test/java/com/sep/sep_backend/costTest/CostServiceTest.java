package com.sep.sep_backend.costTest;

import com.sep.sep_backend.budget.Budget;
import com.sep.sep_backend.budget.BudgetRepository;
import com.sep.sep_backend.cost.Cost;
import com.sep.sep_backend.cost.CostRepository;
import com.sep.sep_backend.cost.CostService;
import com.sep.sep_backend.cost.dto.request.CreateCostRequest;
import com.sep.sep_backend.cost.dto.request.UpdateCostRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CostServiceTest {

    @Mock
    private CostRepository costRepo;

    @Mock
    private BudgetRepository budgetRepo;

    @InjectMocks
    private CostService costService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getAll_returnsAllCosts() {
        Cost c = new Cost();
        c.setAmount(200.0);
        when(costRepo.findByBudget_TravelEntry_UserId("user-1")).thenReturn(List.of(c));

        List<Cost> result = costService.getAll("user-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAmount()).isEqualTo(200.0);
    }

    @Test
    void addCostToBudget_validBudgetId_linksCostToBudget() {
        // 1. GIVEN
        UUID budgetId = UUID.randomUUID();

        Budget budget = new Budget();
        budget.setId(budgetId);
        budget.setName("Urlaub");

        CreateCostRequest request = new CreateCostRequest();
        request.setName("Flug");
        request.setAmount(150.0);

        // Mocking
        when(budgetRepo.findById(budgetId)).thenReturn(Optional.of(budget));

        when(costRepo.save(any(Cost.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 2. WHEN
        Cost result = costService.addCostToBudget(budgetId, request);

        // 3. THEN
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Flug");
        assertThat(result.getAmount()).isEqualTo(150.0);

        assertThat(result.getBudget()).isEqualTo(budget);
        assertThat(result.getBudget().getId()).isEqualTo(budgetId);

        verify(costRepo).save(any(Cost.class));
    }

    @Test
    void addCostToBudget_invalidBudgetId_throwsException() {
        // GIVEN
        UUID budgetId = UUID.randomUUID();

        CreateCostRequest request = new CreateCostRequest();
        request.setName("Test");
        request.setAmount(10.0);

        when(budgetRepo.findById(budgetId)).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() -> costService.addCostToBudget(budgetId, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Budget nicht gefunden mit ID: " + budgetId);
    }

    @Test
    void update_existingId_updatesCost() {
        // 1. GIVEN
        UUID id = UUID.randomUUID();

        // Das bestehende Objekt in der "Datenbank"
        Cost existingCost = new Cost();
        existingCost.setId(id);
        existingCost.setName("Alter Name");
        existingCost.setAmount(100.0);

        // Das Request-DTO mit den neuen Werten
        UpdateCostRequest request = new UpdateCostRequest();
        request.setName("Neuer Name");
        request.setAmount(300.0);

        // Mocking: findById liefert das bestehende Objekt
        when(costRepo.findById(id)).thenReturn(Optional.of(existingCost));

        // Mocking: save gibt einfach das übergebene Objekt zurück
        when(costRepo.save(any(Cost.class))).thenAnswer(i -> i.getArgument(0));

        // 2. WHEN
        Cost result = costService.update(id, request);

        // 3. THEN
        assertThat(result.getAmount()).isEqualTo(300.0);
        assertThat(result.getName()).isEqualTo("Neuer Name");
        assertThat(result.getId()).isEqualTo(id);

        // Sicherstellen, dass save() mit dem richtigen Objekt aufgerufen wurde
        verify(costRepo).save(existingCost);
    }

    @Test
    void update_nonExistingId_throwsException() {
        // GIVEN
        UUID id = UUID.randomUUID();
        UpdateCostRequest request = new UpdateCostRequest();
        request.setName("Test");
        request.setAmount(50.0);

        // Wir mocken findById, das ein leeres Optional zurückgibt
        when(costRepo.findById(id)).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() -> costService.update(id, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Kostenpunkt mit ID " + id + " existiert nicht.");
    }

    @Test
    void delete_callsRepository() {
        UUID id = UUID.randomUUID();
        costService.delete(id);
        verify(costRepo).deleteById(id);
    }
}
