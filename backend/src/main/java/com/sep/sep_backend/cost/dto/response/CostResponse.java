package com.sep.sep_backend.cost.dto.response;

import java.util.UUID;

/**
 * Response body representing a cost item.
 */
public class CostResponse {

    private UUID id;
    private String name;
    private Double amount;
    private UUID budgetId;

    public CostResponse() {
    }

    public CostResponse(UUID id, String name, Double amount, UUID budgetId) {
        this.id = id;
        this.name = name;
        this.amount = amount;
        this.budgetId = budgetId;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public UUID getBudgetId() {
        return budgetId;
    }

    public void setBudgetId(UUID budgetId) {
        this.budgetId = budgetId;
    }
}