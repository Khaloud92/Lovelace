package com.sep.sep_backend.budgetTest;

import com.sep.sep_backend.auth.JwtService;
import com.sep.sep_backend.budget.Budget;
import com.sep.sep_backend.budget.BudgetController;
import com.sep.sep_backend.budget.BudgetService;
import com.sep.sep_backend.budget.dto.request.CreateBudgetRequest;
import com.sep.sep_backend.budget.dto.response.BudgetResponse;
import com.sep.sep_backend.registration.AppUser;
import com.sep.sep_backend.registration.AppUserRepository;
import com.sep.sep_backend.config.SecurityConfig;
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

@WebMvcTest(value = BudgetController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
public class BudgetControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AppUserRepository appUserRepository;

    @MockitoBean
    private BudgetService budgetService;

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

    // --- GET /api/budgets ---

    @Test
    void getAll_shouldReturn200AndBudgetResponseList() throws Exception {
        UUID budgetId = UUID.randomUUID();
        BudgetResponse response = new BudgetResponse(budgetId, "Urlaubsbudget", 500.0);

        when(budgetService.getAllAsResponse(TEST_USER_ID)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/budgets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(budgetId.toString()))
                .andExpect(jsonPath("$[0].name").value("Urlaubsbudget"))
                .andExpect(jsonPath("$[0].amount").value(500.0));
    }

    // --- POST /api/budgets/travel-entry/{travelEntryID} ---

    @Test
    void createBudget_shouldReturn201WithLocationHeader() throws Exception {
        UUID travelEntryId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();

        Budget createdBudget = new Budget();
        createdBudget.setId(budgetId);

        when(budgetService.create(eq(travelEntryId), any(CreateBudgetRequest.class))).thenReturn(createdBudget);

        String requestBody = """
            {
              "name": "Urlaubsbudget Florenz",
              "amount": 750.0
            }
            """;

        mockMvc.perform(post("/api/budgets/travel-entry/" + travelEntryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/budgets/" + budgetId))
                .andExpect(jsonPath("$.id").value(budgetId.toString()));
    }
}