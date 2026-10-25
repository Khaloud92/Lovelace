package com.sep.sep_backend.chat;

import com.sep.sep_backend.registration.AppUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Join entity tracking a member's response to a proposed group trip.
 */
@Getter @Setter
@Entity
@Table(
        name = "group_trip_participants",
        // a user can only respond once per trip
        uniqueConstraints = @UniqueConstraint(columnNames = {"trip_id", "user_id"})
)
public class GroupTripParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private GroupTrip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    /** A member's response state for a proposed trip. */
    public enum Status {
        PENDING,
        ACCEPTED,
        DECLINED
    }
}
