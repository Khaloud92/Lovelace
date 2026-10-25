package com.sep.sep_backend.cost;

import com.sep.sep_backend.budget.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Data access for {@link Cost} entities.
 */
public interface CostRepository extends JpaRepository<Cost, UUID> {

    /** Returns all costs of the given user across all budgets. */
    List<Cost> findByBudget_TravelEntry_UserId(String userId);

    boolean existsByBudgetAndNameAndAmount(Budget budget, String name, Double amount);

    Optional<Cost> findFirstByBudgetAndNameAndAmount(Budget budget, String name, Double amount);
}