package com.sep.sep_backend.chat;

import com.sep.sep_backend.registration.AppUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a trip proposed inside a group chat.
 */
@Getter @Setter
@Entity
@Table(name = "group_trips")
public class GroupTrip {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** The group conversation this trip was proposed in. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    /** The member who proposed the trip. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposed_by", nullable = false)
    private AppUser proposedBy;

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

    private LocalDateTime createdAt;

    /** Accept/decline state of every conversation member for this trip. */
    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GroupTripParticipant> participants = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
