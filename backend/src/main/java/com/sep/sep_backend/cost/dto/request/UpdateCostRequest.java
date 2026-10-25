package com.sep.sep_backend.cost.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Request body for updating a cost.
 */
public class UpdateCostRequest {

    @NotBlank(message = "name is required")
    private String name;

    @NotNull(message = "amount is required")
    @PositiveOrZero(message = "amount must be >= 0")
    private Double amount;

    public UpdateCostRequest() {}

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
}
