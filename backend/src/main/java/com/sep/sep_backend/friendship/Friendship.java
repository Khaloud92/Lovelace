package com.sep.sep_backend.friendship;

import com.sep.sep_backend.registration.AppUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

/**
 * Entity representing a friendship relationship between two users.
 * It manages the state of the friendship (pending, accepted, declined)
 * and ensures that unique relationships are maintained.
 */
@Getter @Setter
@Entity
@Table(
        name = "friendships",
        //constraints prevents multiple friendships between the same two users
        uniqueConstraints = @UniqueConstraint(columnNames = {"requester_id", "addressee_id"})
)
public class Friendship {

    /**
     * The unique identifier for the friendship record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The user who initiated the friendship request.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private AppUser requester;

    /**
     * The user who receives the friendship request.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "addressee_id", nullable = false)
    private AppUser addressee;

    /**
     * The current status of the friendship request. Defaults to PENDING.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    /**
     * Enum defining the possible states of a friendship request.
     */
    public enum Status {
        /**
         * The friendship request has been accepted.
         */
        ACCEPTED,

        /**
         * The friendship request has been declined.
         */
        DECLINED,

        /**
         * The friendship request is waiting for approval from the addressee.
         */
        PENDING
    }
}