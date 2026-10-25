package com.sep.sep_backend.budget;

import com.sep.sep_backend.budget.dto.response.BudgetResponse;
import com.sep.sep_backend.travelentry.TravelEntry;
import com.sep.sep_backend.travelentry.TravelEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.sep.sep_backend.budget.dto.request.CreateBudgetRequest;
import com.sep.sep_backend.budget.dto.request.UpdateBudgetRequest;

import java.util.List;
import java.util.UUID;

/**
 * Business logic for creating, updating, and deleting trip budgets.
 */
@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepo;
    private final TravelEntryRepository travelentryRepo;

    /**
     * Returns all budgets of the given user.
     *
     * @param userId id of the user
     * @return the user's budgets
     */
    public List<Budget> getAll(String userId) {
        return budgetRepo.findByTravelEntry_UserId(userId);
    }

    /**
     * Returns all budgets of the given user mapped to response DTOs.
     *
     * @param userId id of the user
     * @return the user's budgets including trip info
     */
    public List<BudgetResponse> getAllAsResponse(String userId) {
        return getAll(userId).stream()
                .map(b -> new BudgetResponse(
                        b.getId(),
                        b.getName(),
                        b.getAmount(),
                        b.getCosts(),
                        b.getTravelEntry() != null
                                ? new BudgetResponse.TravelEntryInfo(
                                b.getTravelEntry().getId(),
                                b.getTravelEntry().getStartDate().toString(),
                                b.getTravelEntry().getEndDate().toString(),
                                b.getTravelEntry().getColor()
                        )
                                : null
                ))
                .toList();
    }

    /**
     * Creates a budget and links it to a travel entry.
     *
     * @param travelentryID id of the travel entry
     * @param request       the budget data
     * @return the created budget
     */
    public Budget create(UUID travelentryID, CreateBudgetRequest request) {
        TravelEntry travelEntry = travelentryRepo.findById(travelentryID)
                .orElseThrow(() -> new RuntimeException("Travelentry nicht gefunden mit ID: " + travelentryID));

        if (request.getAmount() == null || request.getAmount() < 0) {
            throw new IllegalArgumentException("Please enter a positive Budget amount");
        }
        if (request.getName() == null) {
            throw new IllegalArgumentException("Please enter a name");
        }

        Budget budget = new Budget();
        budget.setAmount(request.getAmount());
        budget.setName(request.getName());
        budget.setTravelEntry(travelEntry);
        Budget savedBudget = budgetRepo.save(budget);

        travelEntry.setBudget(savedBudget);
        travelentryRepo.save(travelEntry);

        return savedBudget;
    }

    /**
     * Updates name and amount of an existing budget.
     *
     * @param id      id of the budget
     * @param request the new values
     * @return the updated budget
     */
    public Budget update(UUID id, UpdateBudgetRequest request) {
        Budget budget = budgetRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Budget not found"));
        if (request.getAmount() == null || request.getAmount() < 0) {
            throw new IllegalArgumentException("Der Amount muss positive sein");
        } else if (request.getName() == null) {
            throw new IllegalArgumentException("Der Name darf nicht leer sein");
        }

        budget.setName(request.getName().trim());
        budget.setAmount(request.getAmount());

        return budgetRepo.save(budget);
    }

    /**
     * Deletes a budget by its id.
     *
     * @param id id of the budget
     */
    public void delete(UUID id) {
        budgetRepo.deleteById(id);
    }
}