package com.sep.sep_backend.costTest;

import com.sep.sep_backend.cost.Cost;
import com.sep.sep_backend.cost.CostController;
import com.sep.sep_backend.cost.CostService;
import com.sep.sep_backend.cost.dto.request.CreateCostRequest;
import com.sep.sep_backend.cost.dto.request.UpdateCostRequest;
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
public class CostControllerTest {

    @Mock
    private CostService costService;

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private CostController controller;

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

    // --- POST /api/costs/budget/{budgetId} ---

    @Test
    void addCostToBudget_shouldReturn200() {
        UUID budgetId = UUID.randomUUID();
        CreateCostRequest request = new CreateCostRequest();
        Cost cost = new Cost();

        when(costService.addCostToBudget(budgetId, request)).thenReturn(cost);

        ResponseEntity<?> response = controller.addCostToBudget(budgetId, request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(cost, response.getBody());
    }

    @Test
    void addCostToBudget_shouldReturn400_whenIllegalArgumentException() {
        UUID budgetId = UUID.randomUUID();
        CreateCostRequest request = new CreateCostRequest();

        when(costService.addCostToBudget(budgetId, request)).thenThrow(new IllegalArgumentException("Invalid data"));

        ResponseEntity<?> response = controller.addCostToBudget(budgetId, request);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Invalid data", response.getBody());
    }

    @Test
    void addCostToBudget_shouldReturn404_whenRuntimeException() {
        UUID budgetId = UUID.randomUUID();
        CreateCostRequest request = new CreateCostRequest();

        when(costService.addCostToBudget(budgetId, request)).thenThrow(new RuntimeException("Budget not found"));

        ResponseEntity<?> response = controller.addCostToBudget(budgetId, request);

        assertEquals(404, response.getStatusCode().value());
    }

    // --- GET /api/costs ---

    @Test
    void getAll_shouldReturnCostsList() {
        stubUserLookup();
        Cost cost = new Cost();
        when(costService.getAll(TEST_USER_ID)).thenReturn(List.of(cost));

        List<Cost> result = controller.getAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(costService).getAll(TEST_USER_ID);
    }

    // --- PUT /api/costs/{id} ---

    @Test
    void update_shouldReturn200() {
        UUID costId = UUID.randomUUID();
        UpdateCostRequest request = new UpdateCostRequest();
        Cost cost = new Cost();

        when(costService.update(costId, request)).thenReturn(cost);

        ResponseEntity<?> response = controller.update(costId, request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(cost, response.getBody());
    }

    @Test
    void update_shouldReturn400_whenIllegalArgumentException() {
        UUID costId = UUID.randomUUID();
        UpdateCostRequest request = new UpdateCostRequest();

        when(costService.update(costId, request)).thenThrow(new IllegalArgumentException("Error"));

        ResponseEntity<?> response = controller.update(costId, request);

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void update_shouldReturn404_whenRuntimeException() {
        UUID costId = UUID.randomUUID();
        UpdateCostRequest request = new UpdateCostRequest();

        when(costService.update(costId, request)).thenThrow(new RuntimeException("Not found"));

        ResponseEntity<?> response = controller.update(costId, request);

        assertEquals(404, response.getStatusCode().value());
    }

    // --- DELETE /api/costs/{id} ---

    @Test
    void delete_shouldReturn204() {
        UUID costId = UUID.randomUUID();

        ResponseEntity<Void> response = controller.delete(costId);

        assertEquals(204, response.getStatusCode().value());
        verify(costService).delete(costId);
    }
}