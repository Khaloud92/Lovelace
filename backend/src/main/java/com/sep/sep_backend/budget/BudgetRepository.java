package com.sep.sep_backend.budget;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Data access for {@link Budget} entities.
 */
public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    /** Returns all budgets of the given user. */
    List<Budget> findByTravelEntry_UserId(String userId);

    Optional<Budget> findByTravelEntry_Id(UUID travelEntryId);
}