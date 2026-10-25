package com.sep.sep_backend.budgetTest;

import com.sep.sep_backend.budget.Budget;
import com.sep.sep_backend.budget.BudgetController;
import com.sep.sep_backend.budget.BudgetService;
import com.sep.sep_backend.budget.dto.request.CreateBudgetRequest;
import com.sep.sep_backend.budget.dto.request.UpdateBudgetRequest;
import com.sep.sep_backend.budget.dto.response.BudgetResponse;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BudgetControllerTest {

    @Mock
    private BudgetService budgetService;

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private BudgetController controller;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_USER_ID = "00000000-0000-0000-0000-000000000001";

    @BeforeEach
    void setUpSecurityContext() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(TEST_EMAIL, null, List.of()));
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void stubUserLookup() {
        AppUser testUser = new AppUser();
        testUser.setId(UUID.fromString(TEST_USER_ID));
        when(appUserRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
    }

    // --- GET /api/budgets ---

    @Test
    void getAll_shouldReturnBudgetResponses() {
        stubUserLookup();

        UUID budgetId = UUID.randomUUID();
        BudgetResponse response = new BudgetResponse(budgetId, "Urlaubsbudget", 500.0);
        when(budgetService.getAllAsResponse(TEST_USER_ID)).thenReturn(List.of(response));

        ResponseEntity<List<BudgetResponse>> result = controller.getAll();

        assertEquals(200, result.getStatusCode().value());
        assertNotNull(result.getBody());
        assertEquals(1, result.getBody().size());
        assertEquals(budgetId, result.getBody().get(0).getId());
        verify(budgetService).getAllAsResponse(TEST_USER_ID);
    }

    // --- POST /api/budgets/travel-entry/{travelEntryID} ---

    @Test
    void createBudget_shouldReturn201WithCreatedBudgetAndLocationHeader() {
        UUID travelEntryId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();

        CreateBudgetRequest request = new CreateBudgetRequest();

        Budget createdBudget = new Budget();
        createdBudget.setId(budgetId);

        when(budgetService.create(travelEntryId, request)).thenReturn(createdBudget);

        ResponseEntity<Budget> response = controller.createBudget(travelEntryId, request);

        assertEquals(201, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(budgetId, response.getBody().getId());
        assertEquals("/api/budgets/" + budgetId, response.getHeaders().getLocation().getPath());
        verify(budgetService).create(travelEntryId, request);
    }

    // --- PUT /api/budgets/{id} ---

    @Test
    void updateBudget_shouldReturn200WithUpdatedBudget() {
        UUID budgetId = UUID.randomUUID();
        UpdateBudgetRequest request = new UpdateBudgetRequest();

        Budget updatedBudget = new Budget();
        updatedBudget.setId(budgetId);

        when(budgetService.update(budgetId, request)).thenReturn(updatedBudget);

        ResponseEntity<Budget> response = controller.updateBudget(budgetId, request);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(budgetId, response.getBody().getId());
        verify(budgetService).update(budgetId, request);
    }

    @Test
    void updateBudget_shouldReturn400WhenIllegalArgumentExceptionThrown() {
        UUID budgetId = UUID.randomUUID();
        UpdateBudgetRequest request = new UpdateBudgetRequest();

        when(budgetService.update(budgetId, request)).thenThrow(new IllegalArgumentException("Invalid data"));

        ResponseEntity<Budget> response = controller.updateBudget(budgetId, request);

        assertEquals(400, response.getStatusCode().value());
        assertNull(response.getBody());
        verify(budgetService).update(budgetId, request);
    }

    @Test
    void updateBudget_shouldReturn404WhenRuntimeExceptionThrown() {
        UUID budgetId = UUID.randomUUID();
        UpdateBudgetRequest request = new UpdateBudgetRequest();

        when(budgetService.update(budgetId, request)).thenThrow(new RuntimeException("Budget not found"));

        ResponseEntity<Budget> response = controller.updateBudget(budgetId, request);

        assertEquals(404, response.getStatusCode().value());
        assertNull(response.getBody());
        verify(budgetService).update(budgetId, request);
    }

    // --- DELETE /api/budgets/{id} ---

    @Test
    void delete_shouldReturn204() {
        UUID budgetId = UUID.randomUUID();

        ResponseEntity<Void> response = controller.delete(budgetId);

        assertEquals(204, response.getStatusCode().value());
        assertNull(response.getBody());
        verify(budgetService).delete(budgetId);
    }
}