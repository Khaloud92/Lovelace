package com.sep.sep_backend.chat;

import com.sep.sep_backend.registration.AppUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Join entity representing a user's membership within a specific conversation.
 */
@Getter @Setter
@Entity
@Table(
        name = "conversation_members",
        // a user can only be in a conversation once
        uniqueConstraints = @UniqueConstraint(columnNames = {"conversation_id", "user_id"})
)
public class ConversationMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** The conversation the user belongs to. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    /** The participant user. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    private LocalDateTime joinedAt;

    @PrePersist
    public void prePersist() {
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }
    }
}