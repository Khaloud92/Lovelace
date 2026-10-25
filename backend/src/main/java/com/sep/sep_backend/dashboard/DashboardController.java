package com.sep.sep_backend.dashboard;

import com.sep.sep_backend.budget.Budget;
import com.sep.sep_backend.budget.BudgetService;
import com.sep.sep_backend.calendar.CalendarEntryService;
import com.sep.sep_backend.dashboard.dto.DashboardSummaryDto;
import com.sep.sep_backend.registration.AppUserRepository;
import com.sep.sep_backend.travelentry.TravelEntryService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoint for the aggregated dashboard summary.
 */
@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final TravelEntryService travelEntryService;
    private final BudgetService budgetService;
    private final CalendarEntryService calendarEntryService;
    private final AppUserRepository appUserRepository;

    public DashboardController(TravelEntryService travelEntryService,
                               BudgetService budgetService,
                               CalendarEntryService calendarEntryService,
                               AppUserRepository appUserRepository) {
        this.travelEntryService = travelEntryService;
        this.budgetService = budgetService;
        this.calendarEntryService = calendarEntryService;
        this.appUserRepository = appUserRepository;
    }

    /**
     * Returns the authenticated user's trip, budget, and event totals.
     *
     * @return the dashboard summary
     */
    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryDto> getDashboardSummary() {
        String userId = resolveUserId();

        int tripsCount = travelEntryService.findAll(userId).size();
        int eventsCount = calendarEntryService.getAllEntries(userId).size();

        List<Budget> allBudgets = budgetService.getAll(resolveUserId());
        double totalBudget = 0.0;
        for (Budget b : allBudgets) {
            if (b.getAmount() != null) {
                totalBudget += b.getAmount();
            }
        }

        int documentsCount = 0;

        DashboardSummaryDto summary = new DashboardSummaryDto(
                tripsCount,
                documentsCount,
                totalBudget,
                eventsCount
        );

        return ResponseEntity.ok(summary);
    }

    private String resolveUserId() {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return appUserRepository.findByEmail(email)
                .map(user -> user.getId().toString())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }
}
