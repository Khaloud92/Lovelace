package com.sep.sep_backend.chat;

import com.sep.sep_backend.registration.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing {@link Conversation} entities and querying chat participations.
 */
@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    /**
     * Finds all conversations where the specified user is registered as a member.
     */
    @Query("""
            SELECT cm.conversation FROM ConversationMember cm
            WHERE cm.user = :me
            """)
    List<Conversation> findAllForUser(@Param("me") AppUser me);

    /**
     * Finds an existing direct 1:1 conversation containing exactly both specified users.
     */
    @Query("""
            SELECT cm.conversation FROM ConversationMember cm
            WHERE cm.conversation.type = :type
              AND cm.user IN (:a, :b)
            GROUP BY cm.conversation
            HAVING COUNT(cm) = 2
            """)
    Optional<Conversation> findDirectBetween(@Param("a") AppUser a,
                                             @Param("b") AppUser b,
                                             @Param("type") Conversation.Type type);
}