package com.sep.sep_backend.cost;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.sep.sep_backend.budget.Budget;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * A single cost item belonging to a budget.
 */
@Getter @Setter
@Entity
public class Cost {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "budget_id")
    @JsonBackReference
    private Budget budget;

    private String name;
    private Double amount;

}
