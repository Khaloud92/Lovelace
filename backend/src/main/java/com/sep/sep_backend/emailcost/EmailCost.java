package com.sep.sep_backend.emailcost;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A cost imported from an email, optionally assigned to a travel entry.
 */
@Getter
@Setter
@Entity
@Table(name = "email_costs")
public class EmailCost {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency = "EUR";

    private String category;

    private String subject;

    private String source;

    private String externalMessageId;

    @Column(length = 4000)
    private String emailText;

    @Column(nullable = false)
    private String userId;

    private UUID travelEntryId;

    private LocalDateTime importedAt;

    private boolean ignored = false;

    @PrePersist
    public void prePersist() {
        if (importedAt == null) {
            importedAt = LocalDateTime.now();
        }

        if (currency == null || currency.isBlank()) {
            currency = "EUR";
        }
    }
}