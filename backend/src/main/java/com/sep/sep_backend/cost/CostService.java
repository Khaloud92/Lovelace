package com.sep.sep_backend.cost;

import com.sep.sep_backend.budget.Budget;
import com.sep.sep_backend.budget.BudgetRepository;
import com.sep.sep_backend.cost.dto.request.CreateCostRequest;
import com.sep.sep_backend.cost.dto.request.UpdateCostRequest;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Business logic for creating, updating, and deleting cost items of a budget.
 */
@Service
@RequiredArgsConstructor
public class CostService {

    private final CostRepository costRepo;
    private final BudgetRepository budgetRepo;

    /**
     * Returns all costs belonging to the given user.
     *
     * @param userId id of the user
     * @return the user's costs across all budgets
     */
    public List<Cost> getAll(String userId) {
        return costRepo.findByBudget_TravelEntry_UserId(userId);
    }

    /**
     * Updates name and amount of an existing cost.
     *
     * @param id      id of the cost
     * @param request the new values
     * @return the updated cost
     */
    public Cost update(UUID id, UpdateCostRequest request) {
        Cost cost = costRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Kostenpunkt mit ID " + id + " existiert nicht."));

        if (request.getAmount() == null || request.getAmount() < 0) {
            throw new IllegalArgumentException("Der Amount muss positiv sein");
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Der Name darf nicht leer sein");
        }

        cost.setName(request.getName().trim());
        cost.setAmount(request.getAmount());

        return costRepo.save(cost);
    }

    /**
     * Deletes a cost by its id.
     *
     * @param id id of the cost
     */
    public void delete(UUID id){
        costRepo.deleteById(id);
    }

    /**
     * Creates a new cost and attaches it to a budget.
     *
     * @param budgetID id of the budget
     * @param request  the cost data
     * @return the created cost
     */
    public Cost addCostToBudget (UUID budgetID, CreateCostRequest request){
        Budget budget = budgetRepo.findById(budgetID)
                .orElseThrow(() -> new RuntimeException("Budget nicht gefunden mit ID: " + budgetID));

        if(request.getAmount() == null || request.getAmount() < 0){
            throw new IllegalArgumentException("Der Amount muss positive sein");
        }else if(request.getName() == null){
            throw new IllegalArgumentException("Der Name darf nicht leer sein");
        }

        Cost cost = new Cost();
        cost.setAmount(request.getAmount());
        cost.setName(request.getName());
        cost.setBudget(budget);

        return costRepo.save(cost);
    }
}
