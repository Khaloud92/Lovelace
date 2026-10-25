package com.sep.sep_backend.budget.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;


/**
 * Request body for updating a budget.
 */
public class UpdateBudgetRequest {

    @NotBlank(message = "name is required")
    private String name;

    @NotNull(message = "amount is required")
    @PositiveOrZero(message = "amount must be >= 0")
    private Double amount;

    public String getName() {return name;}
    public void setName(String name) {
        this.name = name;
    }

    public Double getAmount() {
        return amount;
    }
    public void setAmount(Double amount) {this.amount = amount;}
}

