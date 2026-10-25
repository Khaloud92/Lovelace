package com.sep.sep_backend.registration;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A registered user account, including verification state, profile picture,
 * and location-sharing data.
 */
@Entity
@Table(name = "app_users")
@Getter
@Setter
@NoArgsConstructor
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    private LocalDateTime createdAt;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean emailVerified = false;

    private String confirmationToken;

    // Base64-encoded image, hence TEXT instead of VARCHAR.
    @Column(columnDefinition = "TEXT")
    private String profilePicture;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean shareLocation = false;

    private Double currentLatitude;

    private Double currentLongitude;

    private LocalDateTime locationUpdatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}