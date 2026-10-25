package com.sep.sep_backend.travelentry.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sep.sep_backend.budget.dto.request.CreateBudgetRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Request body for creating a travel entry including its budget.
 */
@Getter @Setter
public class CreateTravelEntryrequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Destination is required")
    private String destination;

    @NotNull(message = "Start date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private String description;

    private String userId;

    private String color;

    @NotNull(message = "Budget information is required")
    @Valid
    private CreateBudgetRequest budget;
}