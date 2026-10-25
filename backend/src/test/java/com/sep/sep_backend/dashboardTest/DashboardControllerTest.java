package com.sep.sep_backend.dashboardTest;

import com.sep.sep_backend.budget.Budget;
import com.sep.sep_backend.budget.BudgetService;
import com.sep.sep_backend.calendar.CalendarEntryService;
import com.sep.sep_backend.dashboard.DashboardController;
import com.sep.sep_backend.dashboard.dto.DashboardSummaryDto;
import com.sep.sep_backend.registration.AppUser;
import com.sep.sep_backend.registration.AppUserRepository;
import com.sep.sep_backend.travelentry.TravelEntryService;
import com.sep.sep_backend.travelentry.dto.TravelEntryResponse;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private TravelEntryService travelEntryService;

    @Mock
    private BudgetService budgetService;

    @Mock
    private CalendarEntryService calendarEntryService;

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private DashboardController dashboardController;

    private Budget budget1;
    private Budget budget2;
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_USER_ID = "00000000-0000-0000-0000-000000000001";

    @BeforeEach
    void setUp() {
        budget1 = new Budget();
        budget1.setAmount(500.0);

        budget2 = new Budget();
        budget2.setAmount(1000.0);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(TEST_EMAIL, null, List.of()));
        SecurityContextHolder.setContext(context);

        AppUser testUser = new AppUser();
        testUser.setId(UUID.fromString(TEST_USER_ID));
        when(appUserRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturn200WithCorrectSummaryData() {
        when(travelEntryService.findAll(TEST_USER_ID)).thenReturn(List.of(
                mock(TravelEntryResponse.class),
                mock(TravelEntryResponse.class)
        ));
        when(calendarEntryService.getAllEntries(TEST_USER_ID)).thenReturn(List.of());
        //when(budgetService.getAll(resolveUserId())).thenReturn(List.of(budget1));

        ResponseEntity<DashboardSummaryDto> response = dashboardController.getDashboardSummary();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void shouldCountTravelEntriesCorrectly() {
        when(travelEntryService.findAll(TEST_USER_ID)).thenReturn(List.of(
                mock(TravelEntryResponse.class),
                mock(TravelEntryResponse.class),
                mock(TravelEntryResponse.class)
        ));
        when(calendarEntryService.getAllEntries(TEST_USER_ID)).thenReturn(List.of());
        //when(budgetService.getAll(resolveUserId())).thenReturn(List.of());

        DashboardSummaryDto body = dashboardController.getDashboardSummary().getBody();

        assertThat(body).isNotNull();
        assertThat(body.totalTravelEntries()).isEqualTo(3);
    }

    @Test
    void shouldCalculateTotalBudgetCorrectly() {
        when(travelEntryService.findAll(TEST_USER_ID)).thenReturn(List.of());
        when(calendarEntryService.getAllEntries(TEST_USER_ID)).thenReturn(List.of());
        when(budgetService.getAll(TEST_USER_ID)).thenReturn(List.of(budget1, budget2));

        DashboardSummaryDto body = dashboardController.getDashboardSummary().getBody();

        assertThat(body).isNotNull();
        assertThat(body.totalBudget()).isEqualTo(1500.0);
    }

    @Test
    void shouldCountCalendarEventsCorrectly() {
        when(travelEntryService.findAll(TEST_USER_ID)).thenReturn(List.of());
        when(calendarEntryService.getAllEntries(TEST_USER_ID)).thenReturn(List.of());
        //when(budgetService.getAll(resolveUserId())).thenReturn(List.of());

        DashboardSummaryDto body = dashboardController.getDashboardSummary().getBody();

        assertThat(body).isNotNull();
        assertThat(body.upcomingCalendarEvents()).isEqualTo(0);
    }

    @Test
    void shouldSetDocumentsCountToZero() {
        when(travelEntryService.findAll(TEST_USER_ID)).thenReturn(List.of());
        when(calendarEntryService.getAllEntries(TEST_USER_ID)).thenReturn(List.of());
        //when(budgetService.getAll(resolveUserId())).thenReturn(List.of());

        DashboardSummaryDto body = dashboardController.getDashboardSummary().getBody();

        assertThat(body).isNotNull();
        assertThat(body.uploadedDocumentsCount()).isEqualTo(0);
    }

    @Test
    void shouldIgnoreBudgetWithNullAmount() {
        Budget budgetWithNull = new Budget();
        budgetWithNull.setAmount(null);

        when(travelEntryService.findAll(TEST_USER_ID)).thenReturn(List.of());
        when(calendarEntryService.getAllEntries(TEST_USER_ID)).thenReturn(List.of());
        when(budgetService.getAll(TEST_USER_ID)).thenReturn(List.of(budgetWithNull, budget1));

        DashboardSummaryDto body = dashboardController.getDashboardSummary().getBody();

        assertThat(body).isNotNull();
        assertThat(body.totalBudget()).isEqualTo(500.0);
    }
}
