package com.sep.sep_backend.travelentry.dto;

import com.sep.sep_backend.budget.dto.response.BudgetResponse;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Response body representing a travel entry with its budget.
 */
public class TravelEntryResponse {

    private UUID id;
    private String title;
    private String destination;
    private LocalDate startDate;
    private LocalDate endDate;
    private String description;
    private String userId;
    private String color;
    private BudgetResponse budget;

    public TravelEntryResponse() {}

    public TravelEntryResponse(UUID id, String title, String destination,
                               LocalDate startDate, LocalDate endDate,
                               String description, String userId,
                               String color, BudgetResponse budget) {
        this.id = id;
        this.title = title;
        this.destination = destination;
        this.startDate = startDate;
        this.endDate = endDate;
        this.description = description;
        this.userId = userId;
        this.color = color;
        this.budget = budget;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public BudgetResponse getBudget() { return budget; }
    public void setBudget(BudgetResponse budget) { this.budget = budget; }
}