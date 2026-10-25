package com.sep.sep_backend.travelentry;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sep.sep_backend.Filemanager.Document;
import com.sep.sep_backend.budget.Budget;
import com.sep.sep_backend.calendar.CalendarEntry;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A planned trip with its budget, calendar entries, and documents.
 */
@Getter
@Setter
@Entity
@Table(name = "travel_entries")
public class TravelEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String destination;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(length = 1000)
    private String description;

    @Column(length = 7)
    private String color;

    @OneToOne(mappedBy = "travelEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    private Budget budget;

    @JsonIgnore
    @OneToMany(mappedBy = "travelEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CalendarEntry> calendarEntries = new ArrayList<>();

    @Transient
    private double plannedBudget;

    private String userId;

    @OneToMany(mappedBy = "travelEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Document> documents = new ArrayList<>();

    /** Creates and attaches a budget on the fly when a planned amount is set and none exists yet. */
    public void setPlannedBudget(double plannedBudget) {
        this.plannedBudget = plannedBudget;

        if (this.budget == null && plannedBudget > 0) {
            Budget newBudget = new Budget();
            newBudget.setName(this.destination != null ? this.destination : this.title);
            newBudget.setAmount(plannedBudget);
            newBudget.setTravelEntry(this);
            this.setBudget(newBudget);
        }
    }

    /** Keeps the budget name in sync with the destination. */
    public void setDestination(String destination) {
        this.destination = destination;
        if (this.budget != null) {
            this.budget.setName(destination);
        }
    }
}