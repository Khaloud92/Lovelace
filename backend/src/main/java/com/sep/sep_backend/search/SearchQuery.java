package com.sep.sep_backend.search;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A user's saved search text with usage count, used for search suggestions.
 */
@Getter
@Setter
@Entity
@Table(
        name = "search_queries",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "query_text"})
        }
)
public class SearchQuery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "query_text", nullable = false)
    private String queryText;

    @Column(name = "search_count", nullable = false)
    private long searchCount = 1;

    @Column(name = "last_searched_at", nullable = false)
    private LocalDateTime lastSearchedAt;

    @PrePersist
    @PreUpdate
    public void updateTimestamp() {
        lastSearchedAt = LocalDateTime.now();
    }
}