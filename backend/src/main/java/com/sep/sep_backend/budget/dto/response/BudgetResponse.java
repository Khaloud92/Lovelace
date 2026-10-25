package com.sep.sep_backend.budget.dto.response;

import com.sep.sep_backend.cost.Cost;

import java.util.List;
import java.util.UUID;

/**
 * Response body representing a budget with its costs and trip info.
 */
public class BudgetResponse {

    private UUID id;
    private String name;
    private Double amount;
    private List<Cost> costs;
    private TravelEntryInfo travelEntry;

    /**
     * Condensed travel entry data shown alongside a budget.
     */
    public static class TravelEntryInfo {
        private UUID id;
        private String startDate;
        private String endDate;
        private String color;

        public TravelEntryInfo(UUID id, String startDate, String endDate, String color) {
            this.id = id;
            this.startDate = startDate;
            this.endDate = endDate;
            this.color = color;
        }

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }

        public String getStartDate() { return startDate; }
        public void setStartDate(String startDate) { this.startDate = startDate; }

        public String getEndDate() { return endDate; }
        public void setEndDate(String endDate) { this.endDate = endDate; }

        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
    }

    public BudgetResponse() {
    }

    public BudgetResponse(UUID id, String name, Double amount) {
        this.id = id;
        this.name = name;
        this.amount = amount;
    }

    public BudgetResponse(UUID id, String name, Double amount, List<Cost> costs, TravelEntryInfo travelEntry) {
        this.id = id;
        this.name = name;
        this.amount = amount;
        this.costs = costs;
        this.travelEntry = travelEntry;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public List<Cost> getCosts() { return costs; }
    public void setCosts(List<Cost> costs) { this.costs = costs; }

    public TravelEntryInfo getTravelEntry() { return travelEntry; }
    public void setTravelEntry(TravelEntryInfo travelEntry) { this.travelEntry = travelEntry; }
}