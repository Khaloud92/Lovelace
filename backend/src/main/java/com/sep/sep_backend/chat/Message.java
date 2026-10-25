package com.sep.sep_backend.chat;

import com.sep.sep_backend.registration.AppUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a chat message sent within a conversation.
 */
@Getter @Setter
@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** The conversation this message belongs to. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    /** The user who sent the message. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private AppUser sender;

    /** The actual text content of the message. */
    @Column(nullable = false, columnDefinition = "text")
    private String content;

    /** Set when this message carries a group trip proposal. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_trip_id")
    private GroupTrip groupTrip;

    private LocalDateTime sentAt;

    @PrePersist
    public void prePersist() {
        if (sentAt == null) {
            sentAt = LocalDateTime.now();
        }
    }
}