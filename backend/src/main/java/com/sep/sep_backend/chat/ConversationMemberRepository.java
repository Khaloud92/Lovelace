package com.sep.sep_backend.chat;

import com.sep.sep_backend.registration.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for managing {@link ConversationMember} relations.
 */
@Repository
public interface ConversationMemberRepository extends JpaRepository<ConversationMember, UUID> {

    /**
     * Checks if a specific user is a participant in the given conversation.
     * Used for authorization checks before reading or sending messages.
     */
    boolean existsByConversationAndUser(Conversation conversation, AppUser user);
}