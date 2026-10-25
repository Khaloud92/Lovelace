package com.sep.sep_backend.budget;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.sep.sep_backend.cost.Cost;
import com.sep.sep_backend.travelentry.TravelEntry;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

/**
 * Budget of a travel entry, holding the planned amount and its cost items.
 */
@Getter @Setter
@Entity
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "travel_id")
    @JsonBackReference
    private TravelEntry travelEntry;

    private String name;
    private Double amount;

    @OneToMany(mappedBy = "budget", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Cost> costs;
}
