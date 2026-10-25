package com.sep.sep_backend.chatTest;

import com.sep.sep_backend.chat.Conversation;
import com.sep.sep_backend.chat.ConversationMember;
import com.sep.sep_backend.registration.AppUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class ConversationMemberTest {

    // --- PrePersist Lifecycle Hook ---

    @Test
    void prePersist_shouldSetJoinedAtWhenNull() {
        // Arrange
        ConversationMember member = new ConversationMember();
        member.setConversation(mock(Conversation.class));
        member.setUser(mock(AppUser.class));

        assertNull(member.getJoinedAt());

        // Act
        member.prePersist();

        // Assert
        assertNotNull(member.getJoinedAt());
        // Überprüft, ob das gesetzte Datum extrem nah an "jetzt" liegt (max. 1 Sekunde Abweichung)
        assertTrue(member.getJoinedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
        assertTrue(member.getJoinedAt().isAfter(LocalDateTime.now().minusSeconds(1)));
    }

    @Test
    void prePersist_shouldNotOverrideJoinedAtWhenAlreadySet() {
        // Arrange
        ConversationMember member = new ConversationMember();
        member.setConversation(mock(Conversation.class));
        member.setUser(mock(AppUser.class));

        LocalDateTime customTime = LocalDateTime.now().minusDays(5);
        member.setJoinedAt(customTime);

        // Act
        member.prePersist();

        // Assert
        assertNotNull(member.getJoinedAt());
        assertEquals(customTime, member.getJoinedAt());
    }
}