package com.sep.sep_backend.budget;

import com.sep.sep_backend.budget.dto.response.BudgetResponse;
import com.sep.sep_backend.registration.AppUserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.sep.sep_backend.budget.dto.request.CreateBudgetRequest;
import com.sep.sep_backend.budget.dto.request.UpdateBudgetRequest;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * REST endpoints for managing trip budgets.
 */
@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class BudgetController {

    private final BudgetService budgetService;
    private final AppUserRepository appUserRepository;

    /**
     * Returns all budgets of the authenticated user.
     *
     * @return the user's budgets
     */
    @GetMapping
    public ResponseEntity<List<BudgetResponse>> getAll() {
        List<BudgetResponse> budgets = budgetService.getAllAsResponse(resolveUserId());
        return ResponseEntity.ok(budgets);
    }

    /**
     * Creates a budget for a travel entry.
     *
     * @param travelEntryID id of the travel entry
     * @param request       the budget data
     * @return the created budget
     */
    @PostMapping("/travel-entry/{travelEntryID}")
    public ResponseEntity<Budget> createBudget(
            @PathVariable UUID travelEntryID,
            @Valid @RequestBody CreateBudgetRequest request) {
        Budget createdBudget = budgetService.create(travelEntryID, request);
        return ResponseEntity
                .created(URI.create("/api/budgets/" + createdBudget.getId()))
                .body(createdBudget);
    }

    /**
     * Updates an existing budget.
     *
     * @param id      id of the budget
     * @param request the new values
     * @return the updated budget, or an error response
     */
    @PutMapping("/{id}")
    public ResponseEntity<Budget> updateBudget(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBudgetRequest request) {
        try {
            return ResponseEntity.ok(budgetService.update(id, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Deletes a budget.
     *
     * @param id id of the budget
     * @return 204 on success
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        budgetService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private String resolveUserId() {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return appUserRepository.findByEmail(email)
                .map(user -> user.getId().toString())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }
}