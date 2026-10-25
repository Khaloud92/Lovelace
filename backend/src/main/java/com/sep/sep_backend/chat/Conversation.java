package com.sep.sep_backend.chat;

import com.sep.sep_backend.registration.AppUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a chat conversation, which can be either a 1:1 direct chat or a group chat.
 */
@Getter @Setter
@Entity
@Table(name = "conversations")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    // null for DIRECT chats, set for GROUP chats
    private String name;

    /** The user who initialized the conversation or created the group. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private AppUser createdBy;

    private LocalDateTime createdAt;

    /** List of all participants belonging to this conversation. */
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ConversationMember> members = new ArrayList<>();

    /**
     * Defines the structural type of the conversation.
     */
    public enum Type {
        DIRECT,
        GROUP
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}