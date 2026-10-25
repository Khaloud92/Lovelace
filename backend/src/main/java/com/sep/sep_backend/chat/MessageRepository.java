package com.sep.sep_backend.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing {@link Message} entities and fetching chat history.
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    /**
     * Retrieves the full chronological message history of a conversation.
     */
    List<Message> findByConversationOrderBySentAtAsc(Conversation conversation);

    /**
     * Retrieves the single most recent message of a conversation for preview purposes.
     */
    Optional<Message> findFirstByConversationOrderBySentAtDesc(Conversation conversation);

    /**
     * Retrieves the chat message announcing the given group trip proposal.
     */
    Optional<Message> findFirstByGroupTrip(GroupTrip groupTrip);
}