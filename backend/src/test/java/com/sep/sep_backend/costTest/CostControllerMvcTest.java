package com.sep.sep_backend.costTest;

import com.sep.sep_backend.auth.JwtService;
import com.sep.sep_backend.config.SecurityConfig;
import com.sep.sep_backend.cost.Cost;
import com.sep.sep_backend.cost.CostController;
import com.sep.sep_backend.cost.CostDocumentScanService;
import com.sep.sep_backend.cost.CostService;
import com.sep.sep_backend.cost.dto.request.CreateCostRequest;
import com.sep.sep_backend.cost.dto.request.UpdateCostRequest;
import com.sep.sep_backend.registration.AppUser;
import com.sep.sep_backend.registration.AppUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = CostController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
public class CostControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AppUserRepository appUserRepository;

    @MockitoBean
    private CostService costService;

    @MockitoBean
    private CostDocumentScanService costDocumentScanService;

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

    // --- POST /api/costs/budget/{budgetId} ---

    @Test
    void addCostToBudget_shouldReturn200_whenValidRequest() throws Exception {
        UUID budgetId = UUID.randomUUID();
        Cost cost = new Cost();
        cost.setId(UUID.randomUUID());

        when(costService.addCostToBudget(eq(budgetId), any(CreateCostRequest.class))).thenReturn(cost);

        String requestBody = """
            {
              "name": "Flugticket",
              "amount": 249.99
            }
            """;

        mockMvc.perform(post("/api/costs/budget/" + budgetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cost.getId().toString()));
    }

    @Test
    void addCostToBudget_shouldReturn400_whenValidationFails() throws Exception {
        UUID budgetId = UUID.randomUUID();

        String invalidRequestBody = """
            {
              "name": "",
              "amount": -50.0
            }
            """;

        mockMvc.perform(post("/api/costs/budget/" + budgetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addCostToBudget_shouldReturn404_whenRuntimeExceptionThrown() throws Exception {
        UUID budgetId = UUID.randomUUID();

        when(costService.addCostToBudget(eq(budgetId), any(CreateCostRequest.class)))
                .thenThrow(new RuntimeException("Budget not found"));

        String requestBody = """
            {
              "name": "Hotel",
              "amount": 500.0
            }
            """;

        mockMvc.perform(post("/api/costs/budget/" + budgetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound());
    }

    // --- GET /api/costs ---

    @Test
    void getAll_shouldReturn200AndCostsList() throws Exception {
        Cost cost = new Cost();
        cost.setId(UUID.randomUUID());

        when(costService.getAll(TEST_USER_ID)).thenReturn(List.of(cost));

        mockMvc.perform(get("/api/costs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(cost.getId().toString()));
    }

    // --- PUT /api/costs/{id} ---

    @Test
    void update_shouldReturn200() throws Exception {
        UUID costId = UUID.randomUUID();
        Cost cost = new Cost();
        cost.setId(costId);

        when(costService.update(eq(costId), any(UpdateCostRequest.class))).thenReturn(cost);


        String requestBody = """
            {
              "name": "Flugticket angepasst",
              "amount": 260.0
            }
            """;

        mockMvc.perform(put("/api/costs/" + costId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(costId.toString()));
    }

    // --- DELETE /api/costs/{id} ---

    @Test
    void delete_shouldReturn204() throws Exception {
        UUID costId = UUID.randomUUID();

        doNothing().when(costService).delete(costId);

        mockMvc.perform(delete("/api/costs/" + costId))
                .andExpect(status().isNoContent());

        verify(costService).delete(costId);
    }
}